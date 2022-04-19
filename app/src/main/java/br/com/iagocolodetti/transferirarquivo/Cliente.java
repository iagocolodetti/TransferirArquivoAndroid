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
import java.io.FileNotFoundException;
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

    private MainActivity mainActivity;

    private Socket socket = null;

    private volatile boolean conectado = false;

    private boolean recebendoArquivo = false;
    private Thread enviandoArquivo = null;

    private final int CONNECT_TIMEOUT = 5000;
    private final int TENTATIVAS_RECONEXAO = 5;
    private final int AGUARDAR_PARA_RECONECTAR = 500;
    private final int AGUARDAR_APOS_ENVIO = 2000;
    private final int AGUARDAR_APOS_ENVIO_LOOP = 10;

    public Cliente(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void conectar(String ip, int porta, String diretorio) throws ClienteConectarException {
        if (ip.isEmpty()) {
            throw new ClienteConectarException(mainActivity.getString(R.string.erro_ip_nao_definido));
        }
        if (!Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$").matcher(ip).matches()) {
            throw new ClienteConectarException(mainActivity.getString(R.string.erro_ip_invalido));
        }
        if (porta < MIN_PORTA || porta > MAX_PORTA) {
            throw new ClienteConectarException(mainActivity.getString(R.string.erro_porta_fora_do_limite, MIN_PORTA, MAX_PORTA));
        }
        if (!new File(diretorio).exists() && !new File(diretorio).mkdir()) {
            throw new ClienteConectarException(mainActivity.getString(R.string.erro_diretorio));
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
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            socket = null;
            mainActivity.desconectado();
        }
    }

    public void enviarArquivos(ArrayList<Arquivo> arquivos) throws EnviarArquivoException {
        if (!isSocketConnected()) {
            throw new EnviarArquivoException(mainActivity.getString(R.string.erro_enviar_nao_conectado));
        }
        if (arquivos == null || arquivos.isEmpty()) {
            throw new EnviarArquivoException(mainActivity.getString(R.string.erro_enviar_sem_arquivo));
        }
        if (recebendoArquivo || enviandoArquivos()) {
            throw new EnviarArquivoException(mainActivity.getString(R.string.erro_enviar_ja_transferindo));
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

        private String ip;
        private int porta;
        private String diretorio;

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
                    esperar(socket == null ? 0 : AGUARDAR_PARA_RECONECTAR);
                    int tentativas = 0;
                    while (conectado && tentativas < TENTATIVAS_RECONEXAO) {
                        try {
                            socket = null;
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, porta), CONNECT_TIMEOUT);
                            mainActivity.conectado();
                            break;
                        } catch (IllegalArgumentException ex) {
                            mainActivity.exibirMensagem(mainActivity.getString(R.string.erro_ip_porta_incorreto));
                            desconectar();
                            ex.printStackTrace();
                            break;
                        } catch (IOException ex) {
                            tentativas++;
                            ex.printStackTrace();
                        }
                    }
                    if (tentativas == TENTATIVAS_RECONEXAO) {
                        mainActivity.exibirMensagem(mainActivity.getString(R.string.erro_nao_possivel_conectar));
                        desconectar();
                    }
                }
                if (conectado) {
                    InputStream is = null;
                    DataInputStream dis = null;
                    OutputStream out = null;
                    try {
                        is = socket.getInputStream();
                        dis = new DataInputStream(is);
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
                        is = socket.getInputStream();
                        out = new FileOutputStream(diretorio + novoNomeArquivo);
                        mainActivity.permanecerAcordado(true);
                        mainActivity.setStatus(mainActivity.getString(R.string.status_recebendo_arquivo));
                        mainActivity.addAreaLog(mainActivity.getString(R.string.recebendo_arquivo, novoNomeArquivo));
                        byte[] buffer = new byte[1024];
                        long bytesRecebidos = 0;
                        int count;
                        long kb = 0;
                        int progresso;
                        while ((count = is.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                            bytesRecebidos += count;
                            progresso = (int) (++kb * 100 / tamanhokb);
                            mainActivity.setStatusProgress(progresso < 100 ? progresso : 100);
                        }
                        if (bytesRecebidos == tamanho) {
                            mainActivity.addAreaLog(mainActivity.getString(R.string.arquivo_recebido, novoNomeArquivo));
                        } else {
                            mainActivity.addAreaLog(mainActivity.getString(R.string.arquivo_recebido_corrompido, novoNomeArquivo));
                            mainActivity.exibirMensagem(mainActivity.getString(R.string.erro_conexao_perdida));
                            desconectar();
                        }
                    } catch (IOException ex) {
                        if (conectado && !enviandoArquivos()) {
                            mainActivity.exibirMensagem(mainActivity.getString(R.string.erro_conexao_perdida));
                            desconectar();
                            ex.printStackTrace();
                        }
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (is != null) {
                                is.close();
                            }
                            if (dis != null) {
                                dis.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
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

        private final ArrayList<Arquivo> arquivos;

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
                    mainActivity.exibirMensagem(mainActivity.getString(R.string.erro_nao_possivel_enviar));
                    break;
                }
                InputStream is = null;
                DataOutputStream dos = null;
                OutputStream out = null;
                try {
                    is = arquivo.getInputStream();
                    dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF(arquivo.getName() + "|" + arquivo.getSize());
                    esperar(1000);
                    out = socket.getOutputStream();
                    mainActivity.permanecerAcordado(true);
                    mainActivity.setStatus(mainActivity.getString(R.string.status_enviando_arquivo));
                    mainActivity.addAreaLog(mainActivity.getString(R.string.enviando_arquivo, arquivo.getName()));
                    byte[] buffer = new byte[1024];
                    long bytesEnviados = 0;
                    int count;
                    long tamanhokb = (arquivo.getSize() / 1024);
                    tamanhokb = (tamanhokb == 0 ? 1 : tamanhokb);
                    long kb = 0;
                    int progresso;
                    while ((count = is.read(buffer)) > 0) {
                        out.write(buffer, 0, count);
                        bytesEnviados += count;
                        progresso = (int) (++kb * 100 / tamanhokb);
                        mainActivity.setStatusProgress(progresso < 100 ? progresso : 100);
                    }
                    if (bytesEnviados == arquivo.getSize()) {
                        mainActivity.addAreaLog(mainActivity.getString(R.string.arquivo_enviado, arquivo.getName()));
                    } else {
                        mainActivity.addAreaLog(mainActivity.getString(R.string.arquivo_enviado_corrompido, arquivo.getName()));
                    }
                } catch (FileNotFoundException ex) {
                    mainActivity.addAreaLog(mainActivity.getString(R.string.arquivo_nao_encontrado, arquivo.getName()));
                } catch (IOException ex) {
                    if (conectado) {
                        mainActivity.exibirMensagem(mainActivity.getString(R.string.erro_conexao_perdida));
                        desconectar();
                        ex.printStackTrace();
                    }
                } finally {
                    try {
                        if (is != null) {
                            if (out != null) {
                                out.close();
                            }
                            is.close();
                            if (dos != null) {
                                dos.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        mainActivity.setStatus("");
                        mainActivity.setStatusProgress(0);
                        mainActivity.permanecerAcordado(false);
                    }
                }
                esperar(AGUARDAR_APOS_ENVIO);
            }
        }
    }
}
