package br.com.iagocolodetti.transferirarquivo;

import java.util.ArrayList;

public class ArquivoHelper {

    private static ArrayList<Arquivo> arquivos = null;
    private static long tamanhoTotal = 0;

    public static void clear() {
        arquivos.clear();
        tamanhoTotal = 0;
    }

    public static ArrayList<Arquivo> getArquivos() {
        return arquivos;
    }

    public static void setArquivos(ArrayList<Arquivo> arquivos) {
        ArquivoHelper.arquivos = new ArrayList<>(arquivos);
    }

    public static long getTamanhoTotal() {
        return tamanhoTotal;
    }

    public static void setTamanhoTotal(Long tamanhoTotal) {
        ArquivoHelper.tamanhoTotal = tamanhoTotal;
    }
}
