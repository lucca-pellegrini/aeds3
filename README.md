# Trabalho Prático de Algoritmos e Estruturas de Dados III

Este repositório contém o Trabalho Prático de Algoritmos e Estruturas de Dados
III (AEDs3) do primeiro semestre de 2025. Ele consiste em um programa, escrito
em [Java 17](https://openjdk.org/projects/jdk/17/), que gerencia uma base de
dados binária contendo dados sobre trilhas (músicas) do Spotify e implementando
operações [CRUD](https://pt.wikipedia.org/w/index.php?title=CRUD). Usamos como
ponto de partida uma base de dados que
[encontramos no Kaggle](https://www.kaggle.com/datasets/olegfostenko/almost-a-million-spotify-tracks),
contendo 899702 registros, dos quais selecionamos apenas aqueles que não contêm
valores nulos, e eliminando certos campos que não são do nosso interesse,
resultando num total de 99890 registros. Veja como os dados são processados
[aqui](https://github.com/lucca-pellegrini/aeds3-dataset).

## Como usar

Antes de começar, certifique-se de ter o seguinte:

- Uma instalação recente de Java, no mínimo Java 17.
- O arquivo JAR, baixado
  [daqui](https://github.com/lucca-pellegrini/aeds3/releases), ou, se preferir
  compilar você mesmo:
  - Instale o [Maven](https://maven.apache.org/)
  - [Clone](https://git-scm.com/book/en/v2) o
    [repositório](https://github.com/lucca-pellegrini/aeds3)
  - Compile o projeto
  ```sh
  git clone --recursive https://github.com/lucca-pellegrini/aeds3
  cd aeds3
  mvn install
  ```
  O arquivo JAR resultante se encontrará na pasta `targets/`

Na execução, será possível importar os dados de
[um arquivo CSV compatível](https://github.com/lucca-pellegrini/aeds3-dataset),
e, tendo inicializado a base de dados binária em disco, todas as operações CRUD
serão disponibilizadas ao usuário.

## Estrutura do Repositório

- **README.md**: este é o arquivo que você está lendo agora.
- **LICENSE**: este arquivo contém a licensa desse programa.
- **pom.xml**: contém as definições relevantes para o
  [Maven](https://maven.apache.org/), ferramenta que escolhemos para automatizar
  a compilação do projeto, além da resolução automática de dependências.
- **src/**: esta pasta contém todo o código-fonte do programa, a implementação
  de toda a funcionalidade e de todos os algoritmos, na qual:
  - **main/<...>/AEDs3/**: contém todo o código-fonte do programa principal:
    - **App.java**: arquivo principal e ponto de partida da execução.
    - **Track.java**: implementação da classe básica, o nosso registro, a trilha
      de música.
    - **TrackDB.java**: implementação das classes relacionadas às operações
      sobre os dados em disco.
    - **CSVManager.java**: implementação de uma classe auxiliar que gerencia a
      leitura e processamento inicial do
      [arquivo CSV compatível](https://github.com/lucca-pellegrini/aeds3-dataset).
    - **Menu.java**: implementação dos menus apresentados ao usuário, e
      responsável por intermediar a interação entre o usuário e o programa.
  - **test/<...>/AEDs3/**: contém todos os testes necessários durante a
    compilação para garantir que o programa se comporta conforme o esperado.
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

## Licença

Este projeto está licenciado sob a licensa [Apache-2.0](LICENSE).
