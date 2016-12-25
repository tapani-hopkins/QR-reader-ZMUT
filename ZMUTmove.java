// NB not finished, currently this is almost the same as ZMUTchange //

import com.github.sarxos.webcam.*;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import java.text.SimpleDateFormat;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.io.*;
import java.util.*;


public class ZMUTmove extends JFrame implements Runnable, ThreadFactory, ActionListener {

	
	private Executor executor = Executors.newSingleThreadExecutor(this);

	private Webcam webcam = null;
	private WebcamPanel panel = null;
	private JTextArea textarea = null;
	public JComboBox cameraList = null;

	public int maxCodes = 1000; // maximum number of codes per file (typically 1000)
	public String [] QRcodes = new String[maxCodes];
	boolean cropImage = true; // if true, the webcam image is cropped so that only the QR code near the centre is read (currently does not work very well!)

	
	// Constructor:
	// Open the webcam, display it in a window, start the program
	public ZMUTmove(int cam) {

		super();

		// Create the window
		setLayout(new BorderLayout(8,4));
		setTitle("QR-koodien lukija (elainmuseo) / QR code reader (ZMUT)");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create the drop down menus, buttons etc. (must be done before opening the camera)
		createControls();
		
		// Open the webcam
		openCam(cam);
		
		// Draw the screen into the window
		drawScreen(true);
		
		// Listen for changes to the drop down menu and start the program
		cameraList.addActionListener(this);
		executor.execute(this);
		
	}

	
	// Main loop: take images every 100 ms and process any QR codes found in them
	@Override
	public void run() {

		String qrstring = "No QR code read yet";
		String textareaText = "\n\n\n\n\n\n\n\n"; // Change this if you want more/less lines of text in the text area (e.g. "\n\n" is 3 lines)
		int codes = 0; // Keep track of how many codes have been read
		int files = 0; // keep track of how many files the codes span
		String filename = "EMPTY FILE created by error in ZMUTmove.csv";
		String filename2 = "EMPTY FILE created by error in ZMUTmove.txt";

		do {
			
			// Pause 100 ms
			try { Thread.sleep(100); } catch(InterruptedException e) { e.printStackTrace(); }
			
			Result result = null;
			BufferedImage image = null;

			// Take an image and try to read any QR code it contains
			if (webcam.isOpen()) {
				
				// Take the image (and skip ahead if no image was acquired)
				if ((image = webcam.getImage()) == null) {
					continue;
				}
				
				// Crop the image so that only Qr codes neara the centre are read
				// NB! Currently does not work well AND you can't see the cropped area on screen!
				if (cropImage){
					image = image.getSubimage(120,80,80,80); // getSubImage(x,y,w,h) , the image is 320 x 240 px
				}
				
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					// Read the QR code
					result = new MultiFormatReader().decode(bitmap);
				} catch (NotFoundException e) {
					// fall through, no QR code in the image
				}
				
			}
			
			// Process the QR code if there was one in the image 
			//	But first check the QR code is not the one saved previously
			//	(i.e. don't do anything if the camera is still pointed at the same code)
			if (result != null && !result.getText().equals(qrstring)) {
				
				// If this code has already been saved, do not save it
				//	NB. The program will only remember the previous ca 1000 codes ( = the value in 'maxCodes')
				// Otherwise save the code to two files and to the screen
				if ( Arrays.asList(QRcodes).contains(result.getText()) ){
					
					textarea.setText( textareaText + "\n\n" + result.getText() + "\nhas already been read" );
					
				} else {
										
					// Create two files to save the codes in if this is the files' first code:
					//	1) a csv file with two header rows, for saving the namespace and ID:s in Kotka format
					//	2) a txt file, for saving a list of Kotka ID:s (used to search for the samples in Kotka)
					if (codes==0){

						files += 1;
						// Include a timestamp in the file names
						String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+"";
						
						// Create the csv file
						try {
							// Give the csv file a name and place it in the "ZMUT codes" folder
							filename = "ZMUT codes/ZMUT codes " + timestamp + ".csv";
							BufferedWriter out = new BufferedWriter(new FileWriter(filename, true));
							out.append("MYNamespaceID,MYObjectID" + "\n");
							out.append("Namespace ID,Object ID" + "\n");
							out.close();
						}
						catch (IOException e)
						{
							System.out.println("Couldn't save the header rows to a csv file.");
						}	
						
						// Create the txt file
						try {
							// Give the txt file the same name as the csv file (but different extension) and place it in the "ZMUT codes" folder
							filename2 = "ZMUT codes/ZMUT codes " + timestamp + ".txt";
							BufferedWriter out = new BufferedWriter(new FileWriter(filename2, true));
							out.close();
						}
						catch (IOException e)
						{
							System.out.println("Couldn't create a text file.");
						}
						
					}
					
					/// Process the QR code and save it in Kotka format to the two files ///
					
					// Add the code to the list (used to check which codes have already been read) 
					QRcodes[codes] = result.getText();
					// Save the code (used to check if the code has changed)
					qrstring = result.getText();
					
					codes += 1;
					
					// Get the Kotka namespace and ID from the QR code:
					//	Split the QR code (using the characters "." and "/" as delimiters), and get the last two parts
					//	e.g. both "ZMUT.12" and "http://mus.utu.fi/ZMUT.12" give {"ZMUT","12"}
					// Then save these in the formats required by the two files:
					//	csv: "ZMUT.12" becomes "utu:ZMUT,12"
					//	txt: "ZMUT.12" becomes "ZMUT.12"
					String [] splittext = result.getText().split("\\.|/");
					String Kotkatext = "";
					String ShortID = "";
					// Try to convert to Kotka format (skip this code if it's not in Kotka format, e.g. there's been a misread)
					try {
						Kotkatext = "utu:" + splittext[splittext.length-2] + "," + splittext[splittext.length-1];
						ShortID = splittext[splittext.length-2] + "." + splittext[splittext.length-1];
					}
					catch (ArrayIndexOutOfBoundsException e1) {
						System.out.println("The Qr code is not in the expected format.");
						System.out.println("Expected something like ZMUT.12 but this code appears to be  " + result.getText());
						continue;
					}

					// Write the QR code to the screen
					//	Drop the last line of text from the screen, then add this code as the new first line
					textareaText = textareaText.substring(0, textareaText.lastIndexOf("\n")); 
					textareaText = files + "." + codes + ":  " + "http://mus.utu.fi/" + ShortID + "\n" + textareaText;
					textarea.setText(textareaText);
				
					// Write the QR code to the csv file
					try {
						BufferedWriter out = new BufferedWriter(new FileWriter(filename, true));
						out.append(Kotkatext + "\n");
						out.close();
					}
					catch (IOException e)
					{
						System.out.println("Couldn't save the QR to a csv file.");
					}
				
					// Write the QR code to the txt file
					try {
						BufferedWriter out = new BufferedWriter(new FileWriter(filename2, true));
						if(codes>1){ out.append(","); }  // separate with commas (but don't start the file with a comma)
						out.append(ShortID);
						out.close();
					}
					catch (IOException e)
					{
						System.out.println("Couldn't save the QR to a text file.");
					}
					
					// If this is the last code that can fit in the files, place the next code in new files
					if (codes>=maxCodes){ codes=0; }
					
				}
				
			}

		} while (true);
	}	// End of main loop
	
	
	// Method for handling changes to drop down menus, button clicks etc
	public void actionPerformed(ActionEvent e) {
		
		// Change the camera if the drop down menu is clicked
        JComboBox cb = (JComboBox)e.getSource();
		int cam = (int) cb.getSelectedIndex();
		changeCam(cam);

    }
	
	
	// Method for changing the active camera
	public void changeCam(int cam){
		
		// Close the old webcam, and remove parts of the screen (e.g. the panel the webcam was displayed in)
		webcam.close();		
		remove(panel);
		remove(textarea);
		remove(cameraList);
		
		// Open the new webcam
		openCam(cam);
		
		// Redraw the screen
		drawScreen(false);
	
	}
	
	
	// Method for opening a webcam
	// int cam:  the webcam to be opened (0 is the default)
	public void openCam(int cam){
	
		try {
			webcam = Webcam.getWebcams().get(cam);
			cameraList.setSelectedIndex(cam);
		} catch (IndexOutOfBoundsException er) {
			// Use the default camera (0) if the chosen webcam does not work
			cam = 0;
			webcam = Webcam.getWebcams().get(cam); 
			cameraList.setSelectedIndex(cam);
			System.out.println("Couldn't find the webcam. Using the default webcam instead.");
		}
		
	}
	
	
	// Method for drawing the screen
	// boolean newScreen:  true draws a new screen, false redraws the existing screen
	public void drawScreen(boolean newScreen){
	
		Dimension size = WebcamResolution.QVGA.getSize();
		webcam.setViewSize(size);
		// NB. for better resolution, try e.g. the following (they are equivalent)
		// webcam.setViewSize(new Dimension(640,480));
		// webcam.setViewSize(WebcamResolution.VGA.getSize());
		
		// Create a text area
		if (newScreen){
			textarea = new JTextArea();
			textarea.setEditable(false);
		}
		textarea.setPreferredSize(size);
		
		// Create a panel to display the webcam in
		panel = new WebcamPanel(webcam);
		panel.setPreferredSize(size);
		
		// Add all the parts to the window and pack them
		add(panel, BorderLayout.LINE_START);
		add(textarea, BorderLayout.CENTER);
		add(cameraList, BorderLayout.PAGE_END);
		pack();

		// Make the window visible
		if (newScreen){
			setVisible(true);
		}
		
	}
	
	
	// Method for creating the drop down menus, buttons etc.
	//	NB these must also be added to drawScreen() and to the start of the class
	public void createControls(){

		// Drop down menu for choosing which camera to use
		cameraList = new JComboBox( Webcam.getWebcams().toArray() );

	}
	
	
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "zmutread");
		t.setDaemon(true);
		return t;
	}

	
	public static void main(String[] args) {
		new ZMUTmove(0);	// Change the starting webcam from here (0 is the default webcam)
	}
	
	
}
