import com.github.sarxos.webcam.*;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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


public class QRreader extends JFrame implements Runnable, ThreadFactory, ActionListener {

	
	private Executor executor = Executors.newSingleThreadExecutor(this);

	public Webcam webcam = null;	
	private WebcamPanel panel = null;
	private JTextArea textarea = null;
	
	public int cam;
	public JComboBox cameraList = null; //new JComboBox( new String [] { "Camera 0", "Camera 1" } );
	
	
	// Create a new screen and open the webcam
	public QRreader(int cam) {
		super();

		setLayout(new BorderLayout(8,4));
		setTitle("QR-koodien lukija / Qr code reader");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		cameraList = new JComboBox( Webcam.getWebcams().toArray() );
		
		// Open the webcam
		try {
			webcam = Webcam.getWebcams().get(cam);
			cameraList.setSelectedIndex(cam);
		} catch (IndexOutOfBoundsException e) {
			// Use the default camera (0) if the chosen webcam does not work
			cam = 0;
			webcam = Webcam.getWebcams().get(cam); 
			cameraList.setSelectedIndex(cam);
			System.out.println("Couldn't find the webcam. Using the default webcam instead.");
		}
		
		// Listen for changes to the drop down menu
		cameraList.addActionListener(this);
		
		Dimension size = WebcamResolution.QVGA.getSize();
		webcam.setViewSize(size);

		panel = new WebcamPanel(webcam);
		panel.setPreferredSize(size);

		textarea = new JTextArea();
		textarea.setEditable(false);
		textarea.setPreferredSize(size);

		add(panel, BorderLayout.LINE_START);
		add(textarea, BorderLayout.CENTER);
		add(cameraList, BorderLayout.PAGE_END);

		pack();
		setVisible(true);

		executor.execute(this);
	}

	
	// Take images every 100 ms and process any QR codes found in them
	@Override
	public void run() {

		String qrstring = "No QR code read yet";
		int codes = 0; // Keep track of how many codes have been read
		
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Result result = null;
			BufferedImage image = null;

			if (webcam.isOpen()) {
				
				if ((image = webcam.getImage()) == null) {
					continue;
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
			
			// Do something if there is a QR code in the image
			if (result != null) {
				
				// Save the text of the QR code to a file if the QR code has changed
				if (!result.getText().equals(qrstring)){
					codes+=1;
					textarea.setText(codes + ":\n" + result.getText());
					try {
						BufferedWriter out = new BufferedWriter(new FileWriter("QR codes.txt", true));
						out.append(result.getText());
						out.append("\n"); 
						out.close();
						qrstring = result.getText(); // save the QR text (used to check if the QR code has changed)
					}
					catch (IOException e)
					{
						System.out.println("Couldn't save the QR to a text file.");
					}
				}
				
			}

		} while (true);
	}

	
	// Change the camera if the drop down menu is clicked
	public void actionPerformed(ActionEvent e) {
		
        JComboBox cb = (JComboBox)e.getSource();
		cam = (int) cb.getSelectedIndex();
		
		// Close the old webcam, and remove parts of the screen (e.g. the panel the webcam was displayed in)
		webcam.close();		
		remove(panel);
		remove(textarea);
		remove(cameraList);
		
		// Open the new webcam
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
		
		// Redraw the screen
		Dimension size = WebcamResolution.QVGA.getSize();
		webcam.setViewSize(size);
		panel = new WebcamPanel(webcam);
		panel.setPreferredSize(size);
		textarea.setPreferredSize(size);
		
		add(panel, BorderLayout.LINE_START);
		add(textarea, BorderLayout.CENTER);
		add(cameraList, BorderLayout.PAGE_END);
		
		pack();
		
    }
	
	
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "QR-reader");
		t.setDaemon(true);
		return t;
	}

	
	public static void main(String[] args) {
		new QRreader(1);	// Change the starting webcam from here (0 is the default webcam)
	}
	
	
}
