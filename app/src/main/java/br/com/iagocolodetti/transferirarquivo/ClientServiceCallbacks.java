package br.com.iagocolodetti.transferirarquivo;

public interface ClientServiceCallbacks {
    void addAreaLog(String message);
    void connecting();
    void connected();
    void disconnected();
    void setStatus(String status);
    void setProgressStatus(int value);
}
