/**
 * Copyright 2013 Chris Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceperman.textcrypt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;

import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


//import java.nio.file.Files;
import java.nio.CharBuffer;
import java.nio.charset.Charset;


import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javax.imageio.ImageIO;
import java.net.URL;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ceperman.textcrypt.CryptUtils.CipherInfo;

/**
 * Encrypts text using AES-256 and bcrypt. 
 * The key setup phase of bcrypt (number of rounds) is variable and adjusted
 * automatically to take ~0.9 sec on the encrypting computer.
 * @author Chris Wood
 */
@SuppressWarnings({ "javadoc", "serial" })
public class TextCrypt extends JFrame implements ActionListener {
	
	// to know the state of the content of the fieldText
	private enum CryptStatus {
	isUNKNOW,
	isENCRYPT,
	isDECRYPT
	}
	private CryptStatus CurrentStatus = CryptStatus.isUNKNOW;
	
	private static Logger logger = Logger.getLogger(TextCrypt.class.getName());
	private boolean D = false; // debugging
	
	private JPasswordField fieldPassword;
	private JCheckBox pswdCheckbox = new JCheckBox();
	private JTextArea fieldText;
	private JTextField txtPath;
	private JButton btnOpen;
	private JButton btnSave;
	private JButton btnEncrypt;
	private JButton btnDecrypt;
	private JButton btnUndo;
	private JCheckBox keyCheckbox = new JCheckBox();
	private JButton btnCopy;
	private JButton btnPaste;
	private JButton btnClear;
	
	private int maxKeyLength;
	private int keyLength;
	
	private static String AppVersion = "0.0";
	
	private static Image icon = null;
	
	
	private class UndoMemory {
		// Private variables to encapsulate the data
		private String oldText;
		private char[] oldPassword;
		private CryptStatus oldStatus;
		// Constructor (optional, but useful for initializing variables)
		public UndoMemory() {
			this.oldText = "";
			this.oldPassword = new char[0];
			this.oldStatus = CryptStatus.isUNKNOW;
		}
		public UndoMemory(String oldText, char[] oldPassword, CryptStatus oldStatus) {
			this.oldText = oldText;
			this.oldPassword = oldPassword.clone();
			this.oldStatus = oldStatus;
		}
		// Getters: methods for accessing variables
		public String getText() {
			return oldText;
		}
		public char[] getPassword() {
			return oldPassword;
		}
		public CryptStatus getStatus() {
			return oldStatus;
		}
		// Setters: methods for modifying variables
		public void setText(String currentText) {
			this.oldText = currentText;
		}
		public void setPassword(char[] currentPassword) {
			java.util.Arrays.fill(this.oldPassword, '\0');
			this.oldPassword = currentPassword.clone();
		}
		public void setStatus(CryptStatus currentStatus) {
			this.oldStatus = currentStatus;
		}
		public void ErasePassword() {
			java.util.Arrays.fill(this.oldPassword, '\0');
		}
	}
	private UndoMemory Undo = new UndoMemory();



	/**
	* Launch the application
	*/
	public static void main(String[] args) {
	  EventQueue.invokeLater(new Runnable() {
		 public void run() {
			ClassLoader classLoader = TextCrypt.class.getClassLoader();
			try (InputStream is = TextCrypt.class.getResourceAsStream("version.properties")) {
				//if (is == null) {
				//	System.out.println("Resource not found!");
				//	return;
				//}
				if (is != null) {
					// Use the input stream (e.g., read properties)
					java.util.Properties props = new java.util.Properties();
					props.load(is);
					AppVersion = props.getProperty("version");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			try {
				TextCrypt frame = new TextCrypt();
				frame.pack();
				frame.setMinimumSize(frame.getPreferredSize());
				
				// centre the window
				//Dimension maxWindow = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
				//            .getSize();
				frame.setSize(500, 600);
				//frame.setLocation(maxWindow.width / 2 - frame.getWidth() / 2, maxWindow.height / 2 - frame.getHeight()
				//            / 2);
				frame.setLocationRelativeTo(null);
				
				// Load image from resources
				try {
					//URL iconURL = DragAndDropFrame.class.getResource("DragAndDropFrame.png");
					URL iconURL = TextCrypt.class.getResource("/file_locked.png");
					if (iconURL != null) {
						//Image icon = ImageIO.read(iconURL);
						icon = ImageIO.read(iconURL);
					} else {
						System.err.println(Messages.getString("icon_failed"));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (icon != null) frame.setIconImage(icon);
				
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		 }
	  });
	}

	/**
	 * Initialize TextCrypt
	 *  Check/enable BouncyCastle crypto provider. 
	 *  Create the UI.
	 */
	public TextCrypt() {
		try {
			CryptUtils.checkBCProvider();
		} catch (NoClassDefFoundError e) {
			JOptionPane.showMessageDialog(this, Messages.getString("no_bcprov"), Messages.getString("no_bcprov_title"),
			JOptionPane.ERROR_MESSAGE);
			System.exit(16);
		}
		
		try {
			maxKeyLength = Math.min(CryptUtils.getMaximumKeyLength(), 256);
		} catch (NoSuchAlgorithmException e1) {
			maxKeyLength = 128;
		}
		keyLength = maxKeyLength;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle(Messages.getString("main_title") + " - v" + AppVersion);
		JPanel cp = new JPanel();
		setContentPane(cp);
		cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));
		cp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		// filepath and buttons
		addFilePath(cp);
		// password stuff
		addPasswordFields(cp);
		// encrypt/decrypt buttons
		addCryptActions(cp);
		// textarea
		addTextArea(cp);
		// copy/paste buttons
		addClipboardActions(cp);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (D) logger.log(Level.INFO, "command is " + event.getActionCommand());
		if (event.getActionCommand().equals("encrypt")) {
			// store to the undo's memory
			UndoMemory tmpMem = new UndoMemory();
			tmpMem.setText(fieldText.getText());
			tmpMem.setPassword(fieldPassword.getPassword());
			tmpMem.setStatus(CurrentStatus);
			
			// show reduced key length if being used
			if (keyLength < 256) {
				keyCheckbox.setSelected(true);
			}
			if (encrypt() == 0) {
				Undo.setText(tmpMem.getText());
				Undo.setPassword(tmpMem.getPassword());
				Undo.setStatus(tmpMem.getStatus());
				tmpMem.ErasePassword(); // to ensure that the memory has been deleted
				
				Undo.setStatus(CryptStatus.isDECRYPT);
				btnDecrypt.setEnabled(true);
				btnEncrypt.setEnabled(false);
				btnUndo.setEnabled(true);
				CurrentStatus = CryptStatus.isENCRYPT;
			}
		}
		else if (event.getActionCommand().equals("decrypt")) {
			// store to the undo's memory
			UndoMemory tmpMem = new UndoMemory();
			tmpMem.setText(fieldText.getText());
			tmpMem.setPassword(fieldPassword.getPassword());
			tmpMem.setStatus(CurrentStatus);
			
			if (decrypt() == 0) {
				Undo.setText(tmpMem.getText());
				Undo.setPassword(tmpMem.getPassword());
				Undo.setStatus(tmpMem.getStatus());
				tmpMem.ErasePassword(); // to ensure that the memory has been deleted
				
				Undo.setStatus(CryptStatus.isENCRYPT);
				btnEncrypt.setEnabled(true);
				btnDecrypt.setEnabled(false);
				btnUndo.setEnabled(true);
				CurrentStatus = CryptStatus.isDECRYPT;
			}
		}
		else if (event.getActionCommand().equals("undo")) {
			btnDecrypt.setEnabled(true);
			btnEncrypt.setEnabled(true);
			if (Undo.getStatus() == CryptStatus.isDECRYPT) {
				btnDecrypt.setEnabled(false);
			}
			if (Undo.getStatus() == CryptStatus.isENCRYPT) {
				btnEncrypt.setEnabled(false);
			}
			btnUndo.setEnabled(false);
			CurrentStatus = Undo.getStatus();
			
			fieldText.setText(Undo.getText());
			//fieldPassword.setPassword(Undo.getPassword());
			fieldPassword.setText(new String(Undo.getPassword()));
			
		}
		else if (event.getActionCommand().equals("copy")) {
			StringSelection stringSelection = new StringSelection(fieldText.getText());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
		}
		else if (event.getActionCommand().equals("paste")) {
			Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			try {
				if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)
					&& transferable.getTransferData(DataFlavor.stringFlavor) != null
					&& ((String) transferable.getTransferData(DataFlavor.stringFlavor)).length() > 0) {
					
					CurrentStatus = CryptStatus.isUNKNOW;
					btnUndo.setEnabled(false);
					fieldText.setText((String) transferable.getTransferData(DataFlavor.stringFlavor));
				}
				else {
					JOptionPane.showMessageDialog(this, Messages.getString("no_data_on_clipboard"),
						Messages.getString("empty_clipboard_title"), JOptionPane.ERROR_MESSAGE);
				}
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, Messages.getString("no_data_on_clipboard"),
					Messages.getString("clipboard_error_title"), JOptionPane.ERROR_MESSAGE);
			}
		} else if (event.getActionCommand().equals("clear")) {
			CurrentStatus = CryptStatus.isUNKNOW;
			fieldText.setText("");
		}
	}

	private void enabledOrDisableBtns() {
		//if (fieldText.getText().length() == 0) {
		if (fieldText.getText().isEmpty()) {
			btnEncrypt.setEnabled(false);
			btnDecrypt.setEnabled(false);
		}
		else {
			if (fieldPassword.getPassword().length == 0) {
				btnEncrypt.setEnabled(false);
				btnDecrypt.setEnabled(false);
			}
			else {
				btnEncrypt.setEnabled(true);
				btnDecrypt.setEnabled(true);
				if (CurrentStatus == CryptStatus.isDECRYPT)
					btnDecrypt.setEnabled(false);
				if (CurrentStatus == CryptStatus.isENCRYPT)
					btnEncrypt.setEnabled(false);
			}
		}
	}

	/*
	 * Encrypt the text.
	 * Preceding the encrypted text is a header:
	 *    salt length
	 *    salt
	 *    bcrypt rounds
	 *    keylength-1
	 * which is used for the decryption.
	 */
	int encrypt() {
		try {
			char[] pswdChars = fieldPassword.getPassword();
			byte[] pswdBytes = Charset.forName("UTF-8").encode(CharBuffer.wrap(pswdChars)).array();
			CipherInfo cipherInfo = CryptUtils
			.createCiphers(pswdBytes, null, 0, keyLength);
			ExpandableByteBuffer bbuf = new ExpandableByteBuffer(32);
			bbuf.put((byte) cipherInfo.salt.length);
			bbuf.put(cipherInfo.salt);
			bbuf.put((byte) cipherInfo.rounds);
			// store the keylength-1 so max keylength (256) will fit in 1 byte
			bbuf.put((byte) (keyLength - 1));
			byte[] encryptedBytes = cipherInfo.encryptCipher.doFinal(fieldText.getText().getBytes("UTF-8"));
			bbuf.put(encryptedBytes);
			String encodedData = Base64.encodeBytes(bbuf.getBytes());
			fieldText.setText(encodedData);
			return 0;
		}
		catch (Exception e) {
			//fieldText.setText(Messages.getString("encryption_failed"));
			JOptionPane.showMessageDialog(this, Messages.getString("encryption_failed"),
			Messages.getString("encryption_error_title"), JOptionPane.ERROR_MESSAGE);
			return -1;
		}
	}

	/*
	 * Decrypt the text, using the info from the header and the password
	 * supplied by the user. 
	 */
	int decrypt() {
		try {
			byte[] decodedBytes = Base64.decode(fieldText.getText().getBytes("UTF-8"));
			byte[] salt = new byte[decodedBytes[0]];
			System.arraycopy(decodedBytes, 1, salt, 0, salt.length);
			int rounds = decodedBytes[salt.length + 1] & 0xFF;
			keyLength = decodedBytes[salt.length + 2] & 0xFF;
			// correct the keylength
			keyLength++;
			if (keyLength < 256) {
				keyCheckbox.setSelected(true);
			} else {
				keyCheckbox.setSelected(false);
			}
			byte[] encryptedBytes = new byte[decodedBytes.length - (salt.length + 3)];
			System.arraycopy(decodedBytes, salt.length + 3, encryptedBytes, 0, encryptedBytes.length);
			char[] pswdChars = fieldPassword.getPassword();
			byte[] pswdBytes = Charset.forName("UTF-8").encode(CharBuffer.wrap(pswdChars)).array();
			CipherInfo cipherInfo = CryptUtils.createCiphers(pswdBytes, salt, rounds,
			keyLength);
			byte[] decryptedBytes = cipherInfo.decryptCipher.doFinal(encryptedBytes);
			fieldText.setText(new String(decryptedBytes, "UTF-8"));
			return 0;
		}
		catch (Exception e) {
			//fieldText.setText(Messages.getString("decryption_failed"));
			JOptionPane.showMessageDialog(this, Messages.getString("decryption_failed"),
			Messages.getString("decryption_error_title"), JOptionPane.ERROR_MESSAGE);
			return -1;
		}
	}

	/*
	 * Add file path and buttons to UI
	 * @param cp
	 */
	private void addFilePath(JPanel cp) {
		JPanel filePathPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filePathPane.add(new JLabel(Messages.getString("txt_file_path"), JLabel.LEFT));
		cp.add(filePathPane);

		JPanel pathPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		txtPath = new JTextField();
		txtPath.setEditable(false);
		// Change text font size
		txtPath.setFont(new Font("Serif",Font.PLAIN,14)); //(new Font("Serif",Font.BOLD,12));
		// Change text font color
		txtPath.setBackground(Color.WHITE);
		txtPath.setForeground(Color.BLACK);
		txtPath.setColumns(23);
		pathPane.add(txtPath);
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(10, 10));
		pathPane.add(spacer);
		btnOpen = new JButton(Messages.getString("open_file"));
		btnOpen.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			//Create a file chooser
			JFileChooser fileChooser = new JFileChooser(){
				@Override
				protected JDialog createDialog( Component parent ) throws HeadlessException {
					JDialog dialog = super.createDialog( parent );
					dialog.setIconImage(icon);
					return dialog;
				}
			};
			
			 // Create a filter for txt file
			FileNameExtensionFilter txtFilter = new FileNameExtensionFilter(
				"TXT Files", "txt"); // Extensions: case-insensitive
			
			// Apply the filter to the file chooser
			fileChooser.setFileFilter(txtFilter);
			
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setPreferredSize(new Dimension(600,600));
			fileChooser.setDialogTitle(btnOpen.getText());
			if (txtPath.getText().isEmpty())
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
			else
				fileChooser.setCurrentDirectory(new File(txtPath.getText()));
			
			int returnVal = fileChooser.showOpenDialog(null);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File thisFiles = fileChooser.getSelectedFile();
				txtPath.setText(thisFiles.getAbsolutePath());
				
				try (BufferedReader reader = new BufferedReader(new FileReader(thisFiles))) {
					StringBuilder content = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						content.append(line).append("\n");
					}
					fieldText.setText(content.toString());
					CurrentStatus = CryptStatus.isUNKNOW;
					btnUndo.setEnabled(false);
					enabledOrDisableBtns();
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(null, "Error opening file: " + ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	});
	pathPane.add(btnOpen);
	JPanel spacer1 = new JPanel();
	spacer1.setPreferredSize(new Dimension(5, 5));
	pathPane.add(spacer1);
	
	btnSave = new JButton(Messages.getString("save_file"));
	btnSave.addActionListener(new ActionListener() {
	@Override
		public void actionPerformed(ActionEvent arg0) {
			if (fieldText.getText().isEmpty()) {
				JOptionPane.showMessageDialog(null, "Text is empty! Nothing to save...",
					"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			//Create a file chooser
			JFileChooser fileChooser = new JFileChooser(){
				@Override
				protected JDialog createDialog( Component parent ) throws HeadlessException {
					JDialog dialog = super.createDialog( parent );
					dialog.setIconImage(icon);
					return dialog;
				}
			};
			 // Create a filter for txt file
			FileNameExtensionFilter txtFilter = new FileNameExtensionFilter(
				"TXT Files", "txt"); // Extensions: case-insensitive
			// Apply the filter to the file chooser
			fileChooser.setFileFilter(txtFilter);
			fileChooser.setPreferredSize(new Dimension(600,600));
			fileChooser.setDialogTitle(btnSave.getText());
			// Define filename with current path
			if (txtPath.getText().isEmpty())
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
			else
				fileChooser.setSelectedFile(new File(txtPath.getText()));
			int option = fileChooser.showSaveDialog(null);
			if (option == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				// Ask user to confirm overwirting the file
				if (file.exists()) {
					int response = JOptionPane.showConfirmDialog(null,
						Messages.getString("confirm_overwrite"), "Confirm Overwrite",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					// User abord the recording
					if (response != JOptionPane.YES_OPTION) {
						return;
					}
				}
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
					writer.write(fieldText.getText());
					//JOptionPane.showMessageDialog(null, "File saved successfully!");
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(null, "Error saving file: " + ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	});
	pathPane.add(btnSave);
	cp.add(pathPane);
	}


	/*
	 * Add password fields to UI
	 * @param cp
	 */
	private void addPasswordFields(JPanel cp) {
		JPanel pswdHdrPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pswdHdrPane.add(new JLabel(Messages.getString("enter_password"), JLabel.LEFT));
		cp.add(pswdHdrPane);
		JPanel pswdPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		fieldPassword = new JPasswordField();
		fieldPassword.setFont(new Font("Serif",Font.PLAIN,14));
		fieldPassword.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				enabledOrDisableBtns();
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				enabledOrDisableBtns();
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				enabledOrDisableBtns();
			}
		});
		fieldPassword.setColumns(23);
		pswdPane.add(fieldPassword);
		
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(10, 10));
		pswdPane.add(spacer);
		pswdPane.add(pswdCheckbox);
		//pswdCheckbox.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 0));
		pswdCheckbox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		pswdCheckbox.setText(Messages.getString("show_password"));
		pswdCheckbox.setPreferredSize(new Dimension(130, 30));
		pswdCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (pswdCheckbox.isSelected()) {
					fieldPassword.setEchoChar((char) 0);
				} else {
					fieldPassword.setEchoChar('*');
				}
			}
		});
		//pswdPane.add(new JLabel(Messages.getString("show_password")));
		cp.add(pswdPane);
	}


	/*
	 * Add encrypt/decrypt actions and key control to UI
	 * @param cp
	 */
	private void addCryptActions(JPanel cp) {
		JPanel cryptButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnEncrypt = new JButton(Messages.getString("encrypt"));
		btnEncrypt.setActionCommand("encrypt");
		btnEncrypt.setEnabled(false); // initially disabled
		btnEncrypt.addActionListener(this);
		cryptButtons.add(btnEncrypt);
		btnDecrypt = new JButton(Messages.getString("decrypt"));
		btnDecrypt.setActionCommand("decrypt");
		btnDecrypt.setEnabled(false); // initially disabled
		btnDecrypt.addActionListener(this);
		cryptButtons.add(btnDecrypt);
		btnUndo = new JButton(Messages.getString("undo"));
		btnUndo.setActionCommand("undo");
		btnUndo.setEnabled(false); // initially disabled
		btnUndo.addActionListener(this);
		cryptButtons.add(btnUndo);
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(30, 10));
		cryptButtons.add(spacer);
		cryptButtons.add(keyCheckbox);
		//keyCheckbox.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 0));
		keyCheckbox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		//keyCheckbox.setBorder(new EmptyBorder(10, 10, 10, 0));
		keyCheckbox.setText(Messages.getString("reduced_keylength"));
		keyCheckbox.setPreferredSize(new Dimension(170, 30));
		keyCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (keyCheckbox.isSelected()) {
					keyLength = 128;
				} else {
					keyLength = maxKeyLength;
				}
			}
		});
		//cryptButtons.add(new JLabel(Messages.getString("reduced_keylength")));
		cp.add(cryptButtons);
	}

	/*
	 * Ad textarea to UI
	 * @param cp
	 */
	private void addTextArea(JPanel cp) {
		JPanel textPane = new JPanel(new BorderLayout());
		textPane.setBorder(BorderFactory.createLineBorder(getBackground(), 5));
		fieldText = new JTextArea();
		fieldText.setFont(new Font("Serif",Font.PLAIN,14));
		fieldText.setLineWrap(true);
		fieldText.setWrapStyleWord(true);
		fieldText.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			// not fired by plain documents
			public void changedUpdate(DocumentEvent e) {
				enabledOrDisableBtns();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				enabledOrDisableBtns();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				enabledOrDisableBtns();
			}
		});
		JScrollPane jScrollPane = new JScrollPane(fieldText);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textPane.add(jScrollPane, BorderLayout.CENTER);
		cp.add(textPane);
	}

	/*
	 * Add clipboard action buttons to UI
	 * @param cp
	 */
	private void addClipboardActions(JPanel cp) {
		JPanel clipboardButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
		//JPanel clipboardButtons = new JPanel(new java.awt.GridLayout(1, 3, 20, 10));
		btnCopy = new JButton(Messages.getString("copy_to_clipboard"));
		btnCopy.setActionCommand("copy");
		btnCopy.addActionListener(this);
		//btnCopy.setPreferredSize(new Dimension(60, 30));
		clipboardButtons.add(btnCopy);
		btnPaste = new JButton(Messages.getString("paste_from_clipboard"));
		btnPaste.setActionCommand("paste");
		btnPaste.addActionListener(this);
		//btnPaste.setPreferredSize(new Dimension(60, 30));
		clipboardButtons.add(btnPaste);
		btnClear = new JButton(Messages.getString("clear_text"));
		btnClear.setActionCommand("clear");
		btnClear.addActionListener(this);
		//btnClear.setPreferredSize(new Dimension(60, 30));
		clipboardButtons.add(btnClear);
		cp.add(clipboardButtons);
	}

}
