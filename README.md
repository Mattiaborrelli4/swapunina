# SwapUnina

## Descrizione
SwapUnina è un progetto Java che utilizza diverse librerie esterne (JavaFX, FFmpeg, Cloudinary, jBCrypt, Apache Commons, ecc.) per funzionare correttamente.  
Questo README spiega come scaricare e configurare tutte le librerie necessarie, sia automaticamente tramite lo script incluso, sia manualmente tramite link diretti.

---

## Prerequisiti

- Java JDK 24 installato e configurato nel PATH: https://www.oracle.com/java/technologies/javase/jdk24-archive-downloads.html
- Apache Maven installato e configurato nel PATH: https://maven.apache.org/download.cgi
- 7-Zip installato (necessario per estrarre file .zip e .7z): https://www.7-zip.org/download.html
- Connessione a Internet per scaricare le librerie

---

## Passo 1: Scaricare le librerie automaticamente

Nel repository è incluso il file `setup_libraries.bat`. Questo script:

- Crea la cartella `librerie` se non esiste
- Scarica e estrae automaticamente:
  - JavaFX SDK 24.0.1
  - FFmpeg 7.1.1 Full Build
- Scarica tutti i `.jar` necessari: jBCrypt, Cloudinary, Apache Commons, HTTP Client, JSON

### Come eseguire lo script

1. Apri il prompt dei comandi nella cartella principale del progetto
2. Esegui il comando:

```bat
setup_libraries.bat
Lo script scaricherà ed estrarrà automaticamente tutte le librerie nella cartella librerie.

Passo 2: Scaricare le librerie manualmente (opzionale)
Se preferisci scaricare le librerie manualmente, ecco tutti i link diretti:

Librerie principali
JavaFX SDK 24.0.1: https://gluonhq.com/products/javafx/

FFmpeg Full Build 7.1.1: https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-full.7z

jBCrypt 0.4: https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar

Cloudinary Core 1.38.0: https://repo1.maven.org/maven2/com/cloudinary/cloudinary-core/1.38.0/cloudinary-core-1.38.0.jar

Cloudinary HTTP44 1.33.0: https://repo1.maven.org/maven2/com/cloudinary/cloudinary-http44/1.33.0/cloudinary-http44-1.33.0.jar

Apache Commons Codec 1.15: https://repo1.maven.org/maven2/commons-codec/commons-codec/1.15/commons-codec-1.15.jar

Apache Commons IO 2.11.0: https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar

Apache Commons Logging 1.2: https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar

Apache HttpClient 4.5.13: https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar

Apache HttpCore 4.4.13: https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.13/httpcore-4.4.13.jar

JSON 20230227: https://repo1.maven.org/maven2/org/json/json/20230227/json-20230227.jar

Passo 3: Estrarre librerie grandi
Alcuni file richiedono estrazione manuale:

JavaFX SDK: viene estratto automaticamente dal .bat in librerie/javafx-sdk-24.0.1

FFmpeg: viene estratto automaticamente dal .bat in librerie/ffmpeg-7.1.1-full_build
Se scaricato manualmente, usa 7-Zip: https://www.7-zip.org/download.html

Passo 4: Configurare Maven e JDK
Assicurati che:

java e javac siano raggiungibili da terminale:

bash
Copia codice
java -version
javac -version
mvn sia raggiungibile:

bash
Copia codice
mvn -version
Struttura del progetto
css
Copia codice
SwapUnina/
│
├─ librerie/             <- Tutte le librerie scaricate
├─ src/                  <- Codice sorgente Java
├─ setup_libraries.bat   <- Script per scaricare ed estrarre automaticamente le librerie
└─ README.md
Avvio del programma
Dopo aver scaricato le librerie e configurato Maven/JDK, puoi compilare ed eseguire il progetto con:

bash
Copia codice
mvn clean install
mvn exec:java -Dexec.mainClass="com.tuo.package.Main"
Sostituisci com.tuo.package.Main con la classe principale del progetto.

Note importanti
Tutti i .jar necessari sono scaricabili automaticamente dallo script .bat

Le librerie pesanti (FFmpeg, JDK, JavaFX) non sono caricate su GitHub a causa dei limiti di dimensione dei file

Assicurati di avere una connessione internet stabile durante l’esecuzione dello script

Il file .bat richiede 7-Zip installato e disponibile nel PATH

Link utili
Java JDK 24: https://www.oracle.com/java/technologies/javase/jdk24-archive-downloads.html

Apache Maven: https://maven.apache.org/download.cgi

7-Zip: https://www.7-zip.org/download.html

JavaFX SDK 24.0.1: https://gluonhq.com/products/javafx/

FFmpeg Full Build 7.1.1: https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-full.7z