# Sistema de Arquivos Distribuído

Este projeto é parte da disciplina de Sistemas Distribuídos, da Universidade Federal do Ceará, e consiste em um simples sistema de arquivos distribuído, usando Java e o Spread toolkit.

São verificados os conceitos de comunicação multicast, tratamento de concorrência, balanceamento, replicação e tolerância a falhas.

#Estrutura do Projeto

As aplicações se dividem em três tipos de processos:
Um grupo de processos clientes, que requisitam operações em arquivos, quais sejam criação (com um número especificado de réplicas), edição, leitura e deleção;
Um grupo de processos slaves, os quais armazenam os arquivos de acordo com a arquitetura do sistema;
E um grupo de processos servidores, que controlam todo o sistema. Nesse grupo, é eleito sempre um dos processos como o Master (aquele com a maior prioridade), o qual é o processo resposável por receber as requisições dos clientes, tratando-as e enviando os comandos para os storages (processos slaves) executarem a persistência dos arquivos, conforme solicitado.

Para que seja possível a comunicação entre os processos, o daemon do spread deve estar executando.


