# QR reader (ZMUT)
QR reader for the Zoological Museum of the University of Turku, Finland. This is used to read barcodes with a webcam, and save the results to a file. Generally the barcodes will be specimen identifiers (e.g. QR codes on insect labels) but other codes can also be read.


## Licence in a nutshell
Use any way you want but please attribute. The reader uses both [webcam-capture](https://github.com/sarxos/webcam-capture) and [Zxing](https://github.com/zxing/zxing) - see the 'Licences' folder for all the relevant licences.


## How to run
With apologies to more experienced java programmers.. I'm not one, nor are most users of this QR reader, so here are the basic instructions:
- download all the files and check java is installed on your computer
- open Terminal (mac users) or cmd (windows users) and go to the folder. 
- run with the `java` command, adding all the files in the `libs` folder to the classpath. For example: 
`java -classpath "libs/webcamread.jar":"libs/bridj-0.6.2.jar":"libs/slf4j-api-1.7.2.jar":"libs/core-2.0.jar":"libs/javase-2.0.jar":"." QRreader`

Here's an an example of how to do it on my Mac, including how to compile the .java files:
```
cd "/Volumes/Data/T/Writings, news articles, scripts, music/Programming/QR reader 2016"

javac -classpath "libs/webcamread.jar":"libs/bridj-0.6.2.jar":"libs/slf4j-api-1.7.2.jar":"libs/core-2.0.jar":"libs/javase-2.0.jar" QRreader.java

java -classpath "libs/webcamread.jar":"libs/bridj-0.6.2.jar":"libs/slf4j-api-1.7.2.jar":"libs/core-2.0.jar":"libs/javase-2.0.jar":"." QRreader

```


## Files
There are four main files. From simplest to most complex:

#### QRreader.java
A simple barcode reader. Reads barcodes from a webcam and saves them into the file 'QR codes.txt'. The contents of the file are not overwritten, so it will need to be emptied manually from time to time.

#### ZMUTread.java
A barcode reader for typical use at the museum. Reads specimen barcodes from a webcam, then saves them in Kotka format (i.e. a format which can be easily loaded to the collection management system [Kotka](https://wiki.helsinki.fi/display/digit/Kotka+Collection+Management+System). It will create two files:
- a .csv file which can be saved as excel and imported into Kotka. This contains the ID:s and namespaces of all the scanned specimens.
- a .txt file with a list of the scanned specimens. This can be pasted into the Kotka search field to view the specimens.

The files are created in the folder 'ZMUT codes' and given a distinct timestamp. New files are created after 1000 specimens (files larger than this cannot be imported into Kotka). 

NB. The reader tries to read only specimen codes, but it may accept *any* barcode shown to the webcam. If you've accidentally scanned e.g. a clothes tag, you can prevent it becoming a formal museum specimen by deleting it from the .csv and .txt file :smiley:

#### ZMUTchange.java
**Not ready yet**

A barcode reader for modifying specimen data in Kotka. (e.g. to add an identification) Reads specimen barcodes from a webcam, then saves them and the changes to be made to them in Kotka format. It will create two files:
- a .csv file which can be saved as excel and imported into Kotka. This contains the ID:s, namespaces and the changed data fields of all the scanned specimens.
- a .txt file with a list of the scanned specimens. This can be pasted into the Kotka search field to view the specimens.

The files are created in the folder 'ZMUT changes' and given a distinct timestamp. New files are created after 1000 specimens (files larger than this cannot be imported into Kotka).

#### ZMUTmove.java
**Not ready yet**

A barcode reader for changing the location data of specimens. Reads the barcodes of specimens, boxes, shelves etc, then saves their new location. (e.g. to move a box of insects to a different shelf or an insect between boxes)
