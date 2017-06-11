# Sistema de Arquivos Distribuído

Este projeto é parte da disciplina de Sistemas Distribuídos, da Universidade Federal do Ceará, e consiste em um simples sistema de arquivos distribuído, usando Java e o Spread toolkit.

São verificados os conceitos de comunicação multicast, tratamento de concorrência, balanceamento, replicação e tolerância a falhas.

# Estrutura do Projeto

As aplicações se dividem em três tipos de processos:
Um grupo de processos clientes, que requisitam operações em arquivos, quais sejam criação (com um número especificado de réplicas), edição, leitura e deleção;
Um grupo de processos slaves, os quais armazenam os arquivos de acordo com a arquitetura do sistema;
E um grupo de processos servidores, que controlam todo o sistema. Nesse grupo, é eleito sempre um dos processos como o Master (aquele com a maior prioridade), o qual é o processo responsável por receber as requisições dos clientes, tratando-as e enviando os comandos para os storages (processos slaves) executarem a persistência dos arquivos, conforme solicitado.

Para que seja possível a comunicação entre os processos, o daemon do spread deve estar executando.

# Configurando o ambiente para rodar o daemon do SPREAD

1. Criar uma pasta "spreadtoolkit" em /home/ubuntu/
    
    cd /home/ubuntu
    mkdir spreadtoolkit
    cd /home/ubuntu/spreadtoolkit

2. Baixar o pacote spread do github
    
    git clone https://github.com/glycerine/spread-src-4.4.0

3. Compilar e instalar o pacote do spread
    
    cd spread-src-4.4.0
    ./configure
    sudo make
    sudo make install

# Executando a aplicação

1. Executar o daemon do spread
    
    ~/spreadtoolkit/spread-src-4.4.0/daemon/spread

2. Depois de executar o spread, abrir outros terminais para exetar os .java do
projeto (pasta src) em cada um.

3. Primeiramente deve ser iniciado um servidor distribuído DServer. Os parâme-
tros são o ID do servidor e sua prioridade. Podem ser instanciados vários servi-
dores para verificar o funcionamento distribuído da aplicação.
    
    cd ~/workspace/src
    java DServer 1 1

4. Depois, em outro terminal, deve ser iniciado um servidor DSlave, o qual é resposável por gerenci-
ar as operações de CRUD do sistema.

    cd ~/workspace/src
    java DSlave 1 1

5. No quarto terminal rodará a aplicação cliente.

    cd ~/workspace/src
    java Client
