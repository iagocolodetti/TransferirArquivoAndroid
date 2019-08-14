/*
 * Copyright (C) 2019 Iago Colodetti
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package br.com.iagocolodetti.transferirarquivo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Pattern;

import br.com.iagocolodetti.transferirarquivo.exception.ClienteConectarException;
import br.com.iagocolodetti.transferirarquivo.exception.EnviarArquivoException;

/**
 *
 * @author iagocolodetti
 */
public class Cliente {

    private final int MIN_PORTA = 0;
    private final int MAX_PORTA = 65535;

    private MainActivity mainActivity = null;

    private Socket socket = null;

    private volatile boolean conectado = false;

    private boolean recebendoArquivo = false;
    private Thread enviandoArquivo = null;

    private final int CONNECT_TIMEOUT = 5000;
    private final int TENTATIVAS_RECONEXAO = 5;
    private final int AGUARDAR_APOS_ENVIO = 2000;
    private final int AGUARDAR_APOS_ENVIO_LOOP = 10;

    public Cliente(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void conectar(String ip, int porta, String diretorio) throws ClienteConectarException {
        if (ip.isEmpty()) {
            throw new ClienteConectarException("Entre com o IP do servidor.");
        }
        if (!Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$").matcher(ip).matches()) {
            throw new ClienteConectarException("Digite um IP válido.");
        }
        if (porta < MIN_PORTA || porta > MAX_PORTA) {
            throw new ClienteConectarException("A porta deve ser no mínimo " + MIN_PORTA + " e no máximo " + MAX_PORTA + ".");
        }
        if (!new File(diretorio).exists() && !new File(diretorio).mkdir()) {
            throw new ClienteConectarException("Não foi possível usar o diretório selecionado.");
        }
        mainActivity.conectando();
        Thread t = new Thread(new ClienteConectar(ip, porta, diretorio));
        t.start();
    }

    public void desconectar() {
        try {
            conectado = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            mainActivity.desconectado();
        }
    }

    public void enviarArquivos(ArrayList<Arquivo> arquivos) throws EnviarArquivoException {
        if (!isSocketConnected()) {
            throw new EnviarArquivoException("É necessário estar conectado a um servidor para enviar arquivos.");
        }
        if (arquivos == null || arquivos.isEmpty()) {
            throw new EnviarArquivoException("Selecione pelo menos um arquivo para enviar.");
        }
        if (recebendoArquivo ||  enviandoArquivos()) {
            throw new EnviarArquivoException("Uma transferência já está em andamento.");
        }
        enviandoArquivo = new Thread(new EnviarArquivos(arquivos));
        enviandoArquivo.start();
    }

    public boolean enviandoArquivos() {
        return (enviandoArquivo != null && enviandoArquivo.isAlive());
    }

    private boolean isSocketConnected() {
        return (socket != null && !socket.isClosed() && socket.isConnected());
    }

    private void esperar(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ClienteConectar implements Runnable {

        private String ip = "";
        private int porta = 0;
        private String diretorio = "";

        public ClienteConectar(String ip, int porta, String diretorio) {
            conectado = true;
            this.ip = ip;
            this.porta = porta;
            this.diretorio = diretorio;
        }

        @Override
        public void run() {
            while (conectado) {
                recebendoArquivo = false;
                if (!isSocketConnected()) {
                    int tentativas = 0;
                    while (conectado && tentativas < TENTATIVAS_RECONEXAO) {
                        try {
                            socket = null;
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, porta), CONNECT_TIMEOUT);
                            mainActivity.conectado();
                            break;
                        } catch (IllegalArgumentException e) {
                            mainActivity.exibirMensagem("Erro: IP e/ou porta incorreto(s).");
                            desconectar();
                            e.printStackTrace();
                            break;
                        } catch (IOException e) {
                            tentativas++;
                            e.printStackTrace();
                        }
                    }
                    if (tentativas == TENTATIVAS_RECONEXAO) {
                        mainActivity.exibirMensagem("Erro: Não foi possível conectar-se a esse servidor.");
                        desconectar();
                    }
                }
                if (conectado) {
                    InputStream in = null;
                    DataInputStream dis = null;
                    OutputStream out = null;
                    try {
                        in = socket.getInputStream();
                        dis = new DataInputStream(in);
                        String[] data = dis.readUTF().split(Pattern.quote("|"));
                        recebendoArquivo = true;
                        esperar(1000);
                        String novoNomeArquivo = data[0];
                        for (int i = 1; new File(diretorio + novoNomeArquivo).exists(); i++) {
                            novoNomeArquivo = data[0].substring(0, data[0].lastIndexOf(".")) + "(" + i + ")" + data[0].substring(data[0].lastIndexOf("."));
                        }
                        long tamanho = Long.parseLong(data[1]);
                        long tamanhokb = (data[1].equals("0") ? 1 : tamanho / 1024);
                        tamanhokb = (tamanhokb == 0 ? 1 : tamanhokb);
                        in = socket.getInputStream();
                        out = new FileOutputStream(diretorio + novoNomeArquivo);
                        mainActivity.permanecerAcordado(true);
                        mainActivity.setStatus("Recebendo Arquivo");
                        mainActivity.addAreaLog("Recebendo Arquivo: \"" + novoNomeArquivo + "\".");
                        byte[] buffer = new byte[1024];
                        long bytesRecebidos = 0;
                        int count;
                        long kb = 0;
                        int progresso;
                        while ((count = in.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                            bytesRecebidos += count;
                            progresso = (int)(++kb * 100 / tamanhokb);
                            mainActivity.setStatusProgress(progresso < 100 ? progresso : 100);
                        }
                        if (bytesRecebidos == tamanho) {
                            mainActivity.addAreaLog("Arquivo \"" + novoNomeArquivo + "\" recebido com sucesso.");
                        } else {
                            mainActivity.addAreaLog("Arquivo \"" + novoNomeArquivo + "\" corrompido durante o recebimento.");
                            mainActivity.exibirMensagem("Erro: Conexão com o servidor perdida.");
                            desconectar();
                        }
                    } catch (IOException e) {
                        if (conectado && !enviandoArquivos()) {
                            mainActivity.exibirMensagem("Erro: Conexão com o servidor perdida.");
                            desconectar();
                            e.printStackTrace();
                        }
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (in != null) {
                                in.close();
                            }
                            if (dis != null) {
                                dis.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            mainActivity.setStatusProgress(0);
                            mainActivity.setStatus("");
                            mainActivity.permanecerAcordado(false);
                        }
                    }
                }
            }
        }
    }

    private class EnviarArquivos implements Runnable {

        private ArrayList<Arquivo> arquivos = null;

        public EnviarArquivos(ArrayList<Arquivo> arquivos) {
            this.arquivos = arquivos;
        }

        @Override
        public void run() {
            for (Arquivo arquivo : arquivos) {
                int loop = 0;
                while (!isSocketConnected() && loop < AGUARDAR_APOS_ENVIO_LOOP) {
                    loop++;
                    esperar(AGUARDAR_APOS_ENVIO);
                }
                if (loop == AGUARDAR_APOS_ENVIO_LOOP) {
                    mainActivity.exibirMensagem("Não foi possível enviar os arquivos restantes.");
                    break;
                }
                if (arquivo.getArquivo().exists()) {
                    DataOutputStream dos = null;
                    FileInputStream fis = null;
                    OutputStream out = null;
                    try {
                        dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF(arquivo.getArquivo().getName() + "|" + arquivo.getArquivo().length());
                        esperar(1000);
                        fis = new FileInputStream(arquivo.getArquivo());
                        out = socket.getOutputStream();
                        mainActivity.permanecerAcordado(true);
                        mainActivity.setStatus("Enviando Arquivo");
                        mainActivity.addAreaLog("Enviando Arquivo: \"" + arquivo.getArquivo().getName() + "\".");
                        byte[] buffer = new byte[1024];
                        long bytesEnviados = 0;
                        int count;
                        long tamanhokb = (arquivo.getArquivo().length() / 1024);
                        tamanhokb = (tamanhokb == 0 ? 1 : tamanhokb);
                        long kb = 0;
                        int progresso;
                        while ((count = fis.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                            bytesEnviados += count;
                            progresso = (int) (++kb * 100 / tamanhokb);
                            mainActivity.setStatusProgress(progresso < 100 ? progresso : 100);
                        }
                        if (bytesEnviados == arquivo.getArquivo().length()) {
                            mainActivity.addAreaLog("Arquivo \"" + arquivo.getArquivo().getName() + "\" enviado com sucesso.");
                        } else {
                            mainActivity.addAreaLog("Arquivo \"" + arquivo.getArquivo().getName() + "\" corrompido durante o envio.");
                        }
                    } catch (IOException e) {
                        if (conectado) {
                            mainActivity.exibirMensagem("Erro: Conexão com o servidor perdida.");
                            desconectar();
                            e.printStackTrace();
                        }
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (fis != null) {
                                fis.close();
                            }
                            if (dos != null) {
                                dos.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            mainActivity.setStatus("");
                            mainActivity.setStatusProgress(0);
                            mainActivity.permanecerAcordado(false);
                        }
                    }
                } else {
                    mainActivity.addAreaLog("Arquivo: \"" + arquivo.getArquivo().getName() + "\" não foi encontrado.");
                }
                esperar(AGUARDAR_APOS_ENVIO);
            }
        }
    }
}
