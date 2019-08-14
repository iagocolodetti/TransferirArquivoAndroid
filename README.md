[Downloads](https://github.com/iagocolodetti/TransferirArquivoAndroid/blob/master/README.md#downloads "Downloads")
<br>
[Descrição](https://github.com/iagocolodetti/TransferirArquivoAndroid/blob/master/README.md#descri%C3%A7%C3%A3o "Descrição")
<br>
[Funcionamento](https://github.com/iagocolodetti/TransferirArquivoAndroid/blob/master/README.md#funcionamento "Funcionamento")
<br>
[Requisitos Mínimos](https://github.com/iagocolodetti/TransferirArquivoAndroid/blob/master/README.md#requisitos-mínimos "Requisitos Mínimos")
<br>
[Projeto](https://github.com/iagocolodetti/TransferirArquivoAndroid/blob/master/README.md#projeto "Projeto")
<br>
<br>
# Downloads
https://github.com/iagocolodetti/TransferirArquivoAndroid/releases
* [TransferirArquivo.apk](https://github.com/iagocolodetti/TransferirArquivoAndroid/releases/download/v1.0/TransferirArquivo.apk "TransferirArquivo.apk")
* [Código-fonte](https://github.com/iagocolodetti/TransferirArquivoAndroid/archive/v1.0.zip "v1.0.zip")
* [Servidor](https://github.com/iagocolodetti/TransferirArquivo/blob/master/README.md#downloads "TransferirArquivo#Downloads")
# Descrição
Projeto desenvolvido com o objetivo de fazer a conexão com um Servidor (PC) para a transferência de arquivos de maneira bidirecional, ou seja, Servidor e Cliente pode tanto receber quanto enviar arquivos.
<br>
<img src="https://github.com/iagocolodetti/imagens/blob/master/transferirarquivoclienteandroid1.jpg" alt="Cliente desconectado" height="750" width="365">
<img src="https://github.com/iagocolodetti/imagens/blob/master/transferirarquivoclienteandroid2.jpg" alt="Cliente conectado" height="750" width="365">
<img src="https://github.com/iagocolodetti/imagens/blob/master/transferirarquivoclienteandroid3.jpg" alt="Tela selecionar arquivos" height="750" width="365">
<br>
<br>

# Funcionamento
Ao ligar o Servidor fornecendo a porta desejada e o diretório para o recebimento de arquivos, o IP do Servidor é exibido no campo "IP".
<br>
Com o IP e porta do Servidor o Cliente será capaz de conectar-se ao Servidor usando o IP e porta do mesmo e claro, selecionando o diretório em que deseja receber arquivos.
<br>
Após a conexão entre Servidor e Cliente for estabelicida, ambos poderão transferir arquivos de maneira bidirecional, porém singular (um arquivo por vez e enquanto estiver recebendo um arquivo, não poderá enviar um arquivo).

# Requisitos Mínimos
Versão: Android 5.1 (Lollipop)
<br>
Resolução: 1280x720 (HD)
<br>
O Servidor (PC) e o Cliente (PC/Android) devem estar conectados à mesma rede.
<br>
A porta selecionada no Servidor deverá estar aberta/liberada localmente.
<br>
<br>
# Projeto
Desenvolvido no Android Studio 3.4.1 usando a linguagem Java, tecnologias Socket, Thread e a biblioteca [Material File Picker](https://github.com/nbsp-team/MaterialFilePicker).
