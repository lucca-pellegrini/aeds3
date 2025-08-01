# Trabalho Prático de Algoritmos e Estruturas de Dados III

[![Release Status](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-release.yml/badge.svg)](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-release.yml)
[![Build Status](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-build.yml/badge.svg)](https://github.com/lucca-pellegrini/aeds3/actions/workflows/maven-build.yml)

Este repositório contém o Trabalho Prático de Algoritmos e Estruturas de Dados
III (AEDs3) do primeiro semestre de 2025. Ele consiste em um programa, escrito
em [Java 17](https://openjdk.org/projects/jdk/17/), que gerencia uma base de
dados binária contendo dados sobre trilhas (músicas) do Spotify e implementando
operações [CRUD](https://pt.wikipedia.org/w/index.php?title=CRUD), ordenação em
disco, índices diretos e índices reversos, compressão e casamento de padrões.
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
  - Instale o [Maven](https://maven.apache.org/)
  - [Clone](https://git-scm.com/book/en/v2) o
    [repositório](https://github.com/lucca-pellegrini/aeds3)
  - Compile o projeto
  ```sh
  git clone --recursive https://github.com/lucca-pellegrini/aeds3
  cd aeds3
  mvn package -DskipTests
  ```
  O arquivo JAR resultante se encontrará na pasta `targets/`.

  Note: o emprego da flag `-DskipTests` pode ser necessário pois os testes
  foram projetados com sistemas Linux em mente, e podem não suceder no Windows
  ou no macOS.

  - O programa pode ser executado com o Maven, utilizando
  ```sh
  mvn exec:java -DskipTests
  ```
  ou diretamente pelo Java, com
  ```sh
  java -jar target/AEDs3-[VERSÃO]-full.jar
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
        - **App.java**: arquivo principal e ponto de partida da execução.
        - **Track.java**: implementação da classe básica, o nosso registro, a trilha
          de música.
        - **TrackDB.java**: implementação das classes relacionadas às operações
          sobre os dados em disco.
        - **Index.java**: definição da interface de índice primário utilizada para
          otimizar leituras em disco.
        - **BTree.java:** implementação da interface `Index` utilizando uma Árvore
          B em disco, carregando na memória apenas as páginas (nós) necessárias
          para realizar cada operação.
        - **HashTableIndex.java:** implementação da interface `Index` utilizando
          uma tabela Hash Extensível em disco.
        - **InvertedListIndex.java:** implementação do índice reverso utilizando
          listas invertidas, possibilitando fazer buscas eficientes por campos de
          tipo textual.
        - **BalancedMergeSort.java**: implementação da ordenação externa por
          [intercalação balanceada _k-way_](https://en.wikipedia.org/wiki/K-way_merge_algorithm?useskin=vector#Heap),
          utilizando um heap para a distribuição inicial dos registros.
        - **Huffman.java**: implementa compressão dos arquivos usando o algoritmo
            de Huffman.
        - **LZW.java**: implementa compressão dos arquivos usando o algoritmo LZW.
        - **BoyerMoore.java**: implementa casamento de padrões utilizando o
            algoritmo de Boyer-Moore.
        - **KMP.java**: implementa casamento de padrões utilizando o
            algoritmo de Knuth-Morris-Pratt.
        - **CSVManager.java**: implementação de uma classe auxiliar que gerencia a
          leitura e processamento inicial do
          [arquivo CSV compatível](https://github.com/lucca-pellegrini/aeds3-dataset).
        - **CommandLineInterface.java**: implementação dos menus apresentados ao
          usuário, e responsável por intermediar a interação entre o usuário e o
          programa.
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

### [Lucca](https://github.com/lucca-pellegrini)

Faço todo o meu desenvolvimento num computador
[Arch Linux](https://archlinux.org/) utilizando o editor de texto
[NeoVim](https://neovim.io/) com uma
[framework customizada](https://nvchad.com/) e o
[OpenJDK Java 23 development kit](https://openjdk.java.net/). Minha edição de
código e meu uso do Git são feitos exclusivamente pela linha de comando.

### [Pedro Vitor](https://github.com/Pedro0826)

Desenvolvo no Windows e no MacOS utilizando o
[VSCode](https://code.visualstudio.com/) e
[OpenJDK Java 23 development kit](https://openjdk.java.net/). Uso o
[Source Control](https://code.visualstudio.com/docs/sourcecontrol/overview) do
VSCode para commitar e fazer o push dos arquivos do GitHub.

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
