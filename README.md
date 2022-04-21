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
* [TransferirArquivo.apk](https://github.com/iagocolodetti/TransferirArquivoAndroid/releases/download/v1.2/TransferirArquivo.apk "TransferirArquivo.apk")
* [Código-fonte](https://github.com/iagocolodetti/TransferirArquivoAndroid/archive/v1.2.zip "v1.2.zip")
* [Servidor](https://github.com/iagocolodetti/TransferirArquivo/blob/master/README.md#downloads "TransferirArquivo#Downloads")
# Descrição
Projeto desenvolvido com o objetivo de fazer a conexão com um Servidor (PC) para a transferência de myFiles de maneira bidirecional, ou seja, Servidor e Cliente pode tanto receber quanto enviar myFiles.
<br>
<img src="https://github.com/iagocolodetti/imagens/blob/master/TransferirArquivoAndroid/v1.1/01.jpg" alt="Cliente desconectado" height="750" width="365">
<img src="https://github.com/iagocolodetti/imagens/blob/master/TransferirArquivoAndroid/v1.1/02.jpg" alt="Cliente conectado" height="750" width="365">
<img src="https://github.com/iagocolodetti/imagens/blob/master/TransferirArquivoAndroid/v1.1/03.jpg" alt="Tela selecionar myFiles" height="750" width="365">
<br>
<br>

# Funcionamento
Ao ligar o Servidor fornecendo a porta desejada e o diretório para o recebimento de myFiles, o IP do Servidor é exibido no campo "IP".
<br>
Com o IP e porta do Servidor o Cliente será capaz de conectar-se ao Servidor usando o IP e porta do mesmo e claro, selecionando o diretório em que deseja receber myFiles.
<br>
Após a conexão entre Servidor e Cliente for estabelicida, ambos poderão transferir myFiles de maneira bidirecional, porém singular (um myFile por vez e enquanto estiver recebendo um myFile, não poderá enviar um myFile).

# Requisitos Mínimos
Versão: Android 7 (Nougat)
<br>
Resolução: 1280x720 (HD)
<br>
O Servidor (PC) e o Cliente (PC/Android) devem estar conectados à mesma rede.
<br>
A porta selecionada no Servidor deverá estar aberta/liberada localmente.
<br>
<br>
# Projeto
Desenvolvido no Android Studio usando a linguagem Java, tecnologias Socket, Thread.
