# Trabalho Prático de Algoritmos e Estruturas de Dados III

[![Release Status](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-release.yml/badge.svg)](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-release.yml)
[![Build Status](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-build.yml/badge.svg)](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-build.yml)

Este repositório contém o Trabalho Prático de Algoritmos e Estruturas de Dados
III (AEDs3) do primeiro semestre de 2025. Ele consiste em um programa, escrito
em [Java 17](https://openjdk.org/projects/jdk/17/), que gerencia uma base de
dados binária contendo dados sobre trilhas (músicas) do Spotify e implementa
operações [CRUD](https://pt.wikipedia.org/w/index.php?title=CRUD), ordenação externa,
índices primários e reversos, compressão de arquivos, criptografia e algoritmos
de casamento de padrões.
Usamos como ponto de partida uma base de dados que [encontramos no
Kaggle](https://www.kaggle.com/datasets/olegfostenko/almost-a-million-spotify-tracks),
contendo 899702 registros, dos quais selecionamos apenas aqueles que não contêm
valores nulos, e eliminando certos campos que não são do nosso interesse,
resultando num total de 99890 registros. Veja como os dados são processados
[aqui](https://github.com/lucca-pellegrini/aeds3-dataset).

## Como usar

Antes de começar, certifique-se de ter o seguinte:

- Uma instalação recente de Java, no mínimo Java 17.
- O arquivo JAR com terminação `-full.jar`, baixado
  [daqui](https://github.com/lucca-pellegrini/aeds3/releases), ou, se preferir
  compilar você mesmo:
  - Instale o [Maven 3.6+](https://maven.apache.org/) e [Java 17+](https://openjdk.org/projects/jdk/17/)
  - [Clone](https://git-scm.com/book/en/v2) o
    [repositório](https://github.com/lucca-pellegrini/aeds3)
  - Compile o projeto
  ```sh
  git clone --recursive https://github.com/lucca-pellegrini/aeds3
  cd aeds3
  mvn package -DskipTests
  ```
  O comando `mvn package` gera dois arquivos JAR na pasta `target/`:
  - `AEDs3-v0.3.5-minimal.jar`: contém apenas o código do projeto
  - `AEDs3-v0.3.5-full.jar`: contém o código e todas as dependências (recomendado)

  Note: o emprego da flag `-DskipTests` pode ser necessário pois alguns testes
  foram projetados com sistemas Linux em mente, e podem não suceder no Windows
  ou no macOS.

  - O programa pode ser executado de duas formas:
  
  **Com Maven (para desenvolvimento):**
  ```sh
  mvn exec:java
  ```
  
  **Diretamente com Java (recomendado):**
  ```sh
  java -jar target/AEDs3-v0.3.5-full.jar
  ```
  Note: se estiver usando algum multiplexador de terminal, como o
  [tmux](https://github.com/tmux/tmux/wiki), pode ser necessário definir a
  seguinte variável de ambiente antes de executar, para exibir a interface
  corretamente:
  ```sh
  export TERM=xterm-256color
  ```

Na execução, será possível importar os dados de
[um arquivo CSV compatível](https://github.com/lucca-pellegrini/aeds3-dataset),
e, tendo inicializado a base de dados binária em disco, todas as operações CRUD
serão disponibilizadas ao usuário. Use o comando `usage` no menu interativo
para mais informações.

## Funcionalidades Implementadas

Este projeto implementa diversos algoritmos e estruturas de dados estudados em AEDs3:

### Gerenciamento de Dados
- **Operações CRUD**: Criação, leitura, atualização e remoção de registros de trilhas musicais
- **Índices Primários**: Árvore B e Tabela Hash Extensível para acesso rápido aos dados
- **Índice Reverso**: Lista invertida para busca eficiente por campos textuais (nome, artista, álbum)
- **Ordenação Externa**: Intercalação balanceada para ordenar grandes volumes de dados em disco

### Compressão de Arquivos
- **Algoritmo de Huffman**: Compressão baseada na frequência dos caracteres nos dados
- **Algoritmo LZW**: Compressão por dicionário adaptativo (Lempel-Ziv-Welch)
- **Empacotamento**: Combinação de múltiplos arquivos em um único pacote comprimido

### Criptografia
- **Cifra de Vigenère**: Criptografia clássica por substituição polialfabética com chave
- **RSA Híbrido**: Sistema criptográfico moderno combinando RSA para chaves e criptografia simétrica para dados

### Busca de Padrões
- **KMP (Knuth-Morris-Pratt)**: Busca eficiente de padrões em texto com pré-processamento
- **Boyer-Moore**: Algoritmo de busca com análise do padrão da direita para a esquerda

## Dependências e Tecnologias

- **Java 17**: Versão mínima necessária do JDK para compilação e execução
- **Maven 3.6+**: Ferramenta de automação de compilação e gerenciamento de dependências
- **JUnit 5**: Framework para implementação e execução de testes unitários
- **Apache Commons CSV**: Biblioteca para processamento eficiente de arquivos CSV
- **JLine3**: Interface de linha de comando avançada com recursos de autocompletar
- **Picocli**: Framework para criação de aplicações de linha de comando robustas

## Estrutura do Repositório

- **README.md**: este é o arquivo que você está lendo agora.
- **LICENSE**: este arquivo contém a licensa desse programa.
- **pom.xml**: contém as definições relevantes para o
  [Maven](https://maven.apache.org/), ferramenta que escolhemos para automatizar
  a compilação do projeto, além da resolução automática de dependências.
- **src/**: esta pasta contém todo o código-fonte do programa, a implementação
  de toda a funcionalidade e de todos os algoritmos, na qual:
    - **main/**: contém tudo que é contido no programa principal:
      - **java/AEDs3/**: contém todo o código-fonte do programa principal:
        - **App.java**: arquivo principal e ponto de partida da execução
        - **CommandLineInterface.java**: implementação da interface de linha de comando e menus interativos
        - **DataBase/**: pacote contendo classes para gerenciamento de dados:
          - **Track.java**: implementação da classe básica representando uma trilha musical
          - **TrackDB.java**: operações CRUD e gerenciamento do banco de dados em disco
          - **CSVManager.java**: importação e processamento de arquivos CSV
          - **BalancedMergeSort.java**: ordenação externa por intercalação balanceada k-way
          - **Index/**: subpacote com implementações de estruturas de indexação:
            - **ForwardIndex.java**: interface para índices primários
            - **BTree.java**: implementação de Árvore B para índice primário em disco
            - **HashTableIndex.java**: implementação de Tabela Hash Extensível
            - **InvertedListIndex.java**: índice reverso com listas invertidas para busca textual
            - **ForwardIndexRegister.java**: registro para índices primários
        - **Compression/**: pacote para compressão e empacotamento de arquivos:
          - **Compressor.java**: classe principal para operações de compressão/descompressão
          - **FilePacker.java**: empacotamento de múltiplos arquivos com verificação de integridade
          - **CompressionType.java**: enumeração dos tipos de compressão disponíveis
          - **Compressors/**: implementações específicas dos algoritmos:
            - **HuffmanCompressor.java**: implementação do algoritmo de Huffman
            - **LZWCompressor.java**: implementação do algoritmo LZW
            - **CopyCompressor.java**: compressor que apenas copia dados (sem compressão)
            - **StreamCompressor.java**: interface base para compressores
            - **BitStream.java**: manipulação de fluxos de bits para compressão
        - **Cryptography/**: pacote para criptografia e segurança de dados:
          - **Vigenere.java**: implementação da cifra de Vigenère
          - **VigenereKey.java**: gerenciamento de chaves para cifra de Vigenère
          - **RSAHybridCryptography.java**: sistema criptográfico RSA híbrido
          - **RSAKeyGenerator.java**: geração de pares de chaves RSA
          - **RSAKeyLoader.java**: carregamento de chaves RSA de arquivos
          - **EncryptionSystem.java**: interface base para sistemas de criptografia
          - **CryptType.java**: enumeração dos tipos de criptografia disponíveis
        - **PatternMatching/**: algoritmos de busca e casamento de padrões:
          - **KMP.java**: implementação do algoritmo de Knuth-Morris-Pratt
          - **BoyerMoore.java**: implementação do algoritmo de Boyer-Moore
      - **resources/**: contém todos os dados e recursos que o programa
          principal precisa para funcionar.
  - **test/**: contém todos as unidades e recursos de testes necessários
      durante a compilação para garantir que o programa se comporta conforme o
      esperado.
- **dataset/**: é um
  [submódulo](https://git-scm.com/book/en/v2/Git-Tools-Submodules) que
  providencia acesso direto ao
  [código usado para baixar e pré-processar](https://github.com/lucca-pellegrini/aeds3-dataset)
  o arquivo CSV que usamos como base. Está aqui meramente por conveniencia e
  para facilitar a vida dos desenvolvedores.
- **scripts/**: como o nome diz, esta pasta contém _scripts_ auxiliares que
  foram escritos para facilitar e acelerar as partes mais maçantes do processo
  de desenvolvimento colaborativo.
- **.github/**: contém _workflows_ próprios do
  [GitHub Actions](https://github.com/features/actions) que utilizamos no
  repositório principal para automatizar a compilação e o teste das classes,
  providenciar os
  [gráficos de dependências](https://docs.github.com/pt/code-security/supply-chain-security/understanding-your-software-supply-chain/about-the-dependency-graph)
  do projeto, e criar os
  [releases](https://github.com/lucca-pellegrini/aeds3/releases) do programa de
  forma automática. Vale ressaltar que estes _workflows_ estão intimamente
  interligados ao
  [repositório original](https://github.com/lucca-pellegrini/aeds3), e é
  improvável que funcionem em quaisquer _forks_ sem uma variedade de
  configurações particulares, incluindo configurações de segurança da sua conta
  no GitHub.
- **.gitignore**: contém definições de arquivos e diretórios
  [ignorados](https://git-scm.com/docs/gitignore) pelo Git.
- **.gitmodules**: contém definições dos
  [submódulos](https://git-scm.com/book/en/v2/Git-Tools-Submodules) usados.
- **.editorconfig**: define [simples padrões](https://editorconfig.org/) de
  formatação consistentes para uma variedade de editores de código.
- **.clang-format**: define padrões de formatação mais complexos para o
  formatador de código
  [ClangFormat](https://clang.llvm.org/docs/ClangFormat.html).

## Ferramentas utilizadas

### Ambiente de Desenvolvimento

Este projeto foi desenvolvido utilizando:

- **Sistemas Operacionais**: Linux (Arch Linux), Windows e macOS
- **IDEs**: [NeoVim](https://neovim.io/) com configuração personalizada e [VSCode](https://code.visualstudio.com/)
- **JDK**: [OpenJDK Java 17+](https://openjdk.java.net/) (versão mínima para compilação)
- **Controle de Versão**: Git via linha de comando e interface do VSCode

### Desenvolvedores

- **[Lucca Pellegrini](https://github.com/lucca-pellegrini)**: Desenvolvimento em ambiente Linux
- **[Pedro Vitor](https://github.com/Pedro0826)**: Desenvolvimento em ambiente Windows/macOS

### Aviso sobre o uso de LLMs

Conforme as orientações dadas em sala de aula sobre o uso de Modelos de
Linguagem de Grande Escala (LLMs) neste trabalho, a seguir está uma visão geral
das partes deste repositório que foram desenvolvidas com a ajuda de LLMs:

- Sugestão de quais ferramentas e bibliotecas utilizar para a implementação das
  diversas partes, como a esclha do [Maven](https://maven.apache.org/) no lugar
  do [Gradle](https://gradle.org/), devido à relativa simplicidade do projeto, a
  escolha da versão do Java, e a escolha da biblioteca
  [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/index.html)
  para ler os registros da base de dados que usamos como ponto de partida.
- Correção de uma variedade de erros relacionados a problemas de permissão
  encontrados na execução dos _workflows_ em `.github/`.
- Correção de problemas nas definições do Maven em `pom.xml`, especialmente no
  que diz respeito à configuração das versões do Java e das dependências.
- Escrita dos arquivos de formatação `.clang-format` e `.editorconfig`.
- Ajuda com alguns dos métodos da biblioteca
  [pandas](https://pandas.pydata.org/) que optamos por usar no lugar do Excel
  para o pré-processamento dos dados em `dataset/`
- Escrita ou ajuda com a escrita de muitos comentários Javadoc usados para gerar
  a [documentação online](https://aeds3.verticordia.com).
- Este aviso que você está lendo, adicionalmente, foi escrito com a ajuda do
  ChatGPT.

## Licença

Este projeto está licenciado sob a licensa [Apache-2.0](LICENSE).
