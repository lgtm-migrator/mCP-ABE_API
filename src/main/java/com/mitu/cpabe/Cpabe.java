/*
 * 
 */
package com.mitu.cpabe;

import com.google.gson.JsonObject;
import com.mitu.abe.*;
import com.mitu.cpabe.policy.LangPolicy;
import com.mitu.utils.exceptions.AttributesNotSatisfiedException;
import com.mitu.utils.exceptions.NoSuchDecryptionTokenFoundException;
import it.unisa.dia.gas.jpbc.Element;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cpabe {
	static String defaultPropertyLocation = "src/main/java/com/mitu/cpabe/default.properties";
	public static JsonObject setup(String[] attrs){
		 return setup(attrs, defaultPropertyLocation);
	}
	/* Set up- takes a list of all possible attributes and the initial parameter and then returns the public 
	*  encryption key and the master key (used to create private keys)
	*/
	/*API RETURN: {publicKey:string, masterKey:string}*/
	public static JsonObject setup(String[] attrs, String parameterPath)
    {
    	var jsonObject = new JsonObject(); 
		byte[] pub_byte, msk_byte;

		AbePub pub = new AbePub();
		AbeMsk msk = new AbeMsk();
		Abe.setup(pub, msk, attrs, parameterPath);

		/* store public-key in JSON object to return to server*/
		pub_byte = SerializeUtils.serializeBswabePub(pub);
		jsonObject.addProperty("publicKey", Base64.getEncoder().encodeToString(pub_byte));

		/* store AbeMsk into masterKeyFile */
		msk_byte = SerializeUtils.serializeBswabeMsk(msk);
		jsonObject.addProperty("masterKey", Base64.getEncoder().encodeToString(msk_byte));
		return jsonObject;
	}

	public static JsonObject keygen(String publicKey, String masterKey, String attr_str){
		return keygen(publicKey, masterKey, attr_str, defaultPropertyLocation);
	}
	/* Takes the public key and master key both serialized as string then return both shares of the user */
	/*API RETURN: {share1:string, share2:string}*/
	public static JsonObject keygen(String publicKey, String masterKey, String attr_str, String propertyLocation)
    {
		AbePub pub;
		AbeMsk msk;

		byte[] pub_byte, msk_byte, prv_bytePart1, prv_bytePart2;

		/* get AbePub from publicKeyFile */
		pub_byte = Base64.getDecoder().decode(publicKey);
		pub = SerializeUtils.unserializeBswabePub(pub_byte, propertyLocation);

		/* get AbeMsk from masterKeyFile */
		msk_byte = Base64.getDecoder().decode(masterKey);
		msk = SerializeUtils.unserializeBswabeMsk(pub, msk_byte);

		String[] attr_arr = null;
		try {
			attr_arr = LangPolicy.parseAttribute(attr_str);
		} catch (Exception ex) {
			Logger.getLogger(Cpabe.class.getName()).log(Level.SEVERE, null, ex);
		}
		assert attr_arr != null;
		AbePrv prv = Abe.keygen(pub, msk, attr_arr);

		var jsonObject = new JsonObject(); 
		/* store AbePrv into return object  */
		prv_bytePart1 = SerializeUtils.serializeBswabePrvPart1(prv.prv1);
		jsonObject.addProperty("share1", Base64.getEncoder().encodeToString(prv_bytePart1));
		prv_bytePart2 = SerializeUtils.serializeBswabePrvPart2(prv.prv2);
		jsonObject.addProperty("share2", Base64.getEncoder().encodeToString(prv_bytePart2));
		return jsonObject;
	}
	public static JsonObject encrypt(String publicKey, String policy, String inputFileSerialized) throws IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
		return encrypt(publicKey, policy, inputFileSerialized, defaultPropertyLocation);
	}

	/* Use the public key to encrypt the given file use the given policy return the byte 
	*	representation of the encrypted file
	*/
	/*API RETURN: {encryptedFile:string}*/
	public static JsonObject encrypt(String publicKey, String policy, String inputFileSerialized, String propertyLocation)
            throws IOException,
			NoSuchAlgorithmException,
            InvalidKeyException,
            IllegalBlockSizeException,
            NoSuchPaddingException,
			BadPaddingException
    {

		AbePub pub;
		AbeCph cph;
		AbeCphKey keyCph;

		byte[] cphBuf;
		byte[] aesBuf;
		byte[] pub_byte;
		Element m;

		byte[] inputFile = Base64.getDecoder().decode(inputFileSerialized);
		/* get AbePub from publicKeyFile */
		pub_byte = Base64.getDecoder().decode(publicKey);
		pub = SerializeUtils.unserializeBswabePub(pub_byte, propertyLocation);

		keyCph = Abe.enc(pub, policy);
		cph = keyCph.cph;
		m = pub.p.getGT().newElement();
		m = keyCph.key.duplicate();

		if (cph == null) {
			System.err.println("Error happened in enc");
			System.exit(0);
		}

		cphBuf = SerializeUtils.bswabeCphSerialize(cph);
		aesBuf = AESCoder.encrypt(m.toBytes(), inputFile);
		var os = Common.writeCpabeData(cphBuf,  aesBuf);
		byte[] encryptedFileBytes = os.toByteArray();
		var jsonObject = new JsonObject();
		jsonObject.addProperty("encryptedFile", Base64.getEncoder().encodeToString( encryptedFileBytes));
		return jsonObject;
	}

	public static JsonObject halfDecrypt(String publicKey, String share1, String encFile, String professionalId) throws NoSuchDecryptionTokenFoundException, IOException, AttributesNotSatisfiedException {
		return  halfDecrypt(publicKey, share1, encFile, professionalId, defaultPropertyLocation);
	}
	/**
	 * mdecrypt - intermediate decryption process done by the revocation server. It checks the revocation list and
	 * only runs decryption if user has the appropriate permission
	 * Returns half decrypted file
	*/
	/*API RETURN: {mDecrypt:string}*/
	public static JsonObject halfDecrypt(String publicKey, String share1, String encFile, String professionalId, String propertyLocation)
            throws AttributesNotSatisfiedException,
            NoSuchDecryptionTokenFoundException,
            IOException {

		byte[] cphBuf;
		byte[] share1_byte;
		byte[] pub_byte, mDecByte;
		byte[][] tmp;

		AbeCph cph;
		AbePub pub;
		AbePrvPart1 privateKeyPart1;
		AbeMDec mDec;

		/* get AbePub from publicKeyFile */
		pub_byte = Base64.getDecoder().decode(publicKey);
		pub = SerializeUtils.unserializeBswabePub(pub_byte, propertyLocation);
		var jsonObject = new JsonObject(); 

		/* read ciphertext */
		tmp = Common.readCpabeData(new ByteArrayInputStream(Base64.getDecoder().decode(encFile)));
		cphBuf = tmp[1];
		cph = SerializeUtils.bswabeCphUnserialize(pub, cphBuf);

		/* get AbePrvPart1 form prvfilePart1 */
		share1_byte = Base64.getDecoder().decode(share1);
		privateKeyPart1 = SerializeUtils.unserializeBswabePrvPart1(pub, share1_byte);

		mDec = Abe.m_dec(pub, privateKeyPart1, cph);
		mDecByte = SerializeUtils.serializeBswabeMDec(mDec);
		jsonObject.addProperty("mDecryptedFile", Base64.getEncoder().encodeToString( mDecByte));
		return jsonObject;
	}

	public static JsonObject decrypt(String publicKey, String share2, String encFile, String mDecFile,
                             String professionalId) throws IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
		return decrypt(publicKey, share2, encFile, mDecFile, professionalId, defaultPropertyLocation);
	}
	/*Use the half decrypted file, the encrypted file and the second part of the decryption key to make the file*/
	/*API RETURN: {decryptedFile:string}*/
	public static JsonObject decrypt(String publicKey, String share2, String encFile, String mDecFile,
                             String professionalId, String propertyLocation)
			throws IOException, // Regular file IO exception
			IllegalBlockSizeException, // Problem from cp-abe jbr library
			InvalidKeyException, // Exception thrown if key was in wrong format
			BadPaddingException, // Exception comes from the AES encryption used under the hood
			NoSuchAlgorithmException, // Thrown in case the environment running the server doesn't have the requested algorithm
			NoSuchPaddingException // Thrown in case a padding is requested but does not exist in environment
	{

		byte[] aesBuf, cphBuf;
        byte[] share2_byte;
		byte[] pub_byte;
		byte[][] tmp;

		AbeCph cph;
		AbePub pub;
		AbePrvPart2 privateKeyPart2;
		AbeMDec mDec = null;

		/* get AbePub from publicKeyFile */
		pub_byte = Base64.getDecoder().decode(publicKey);
		pub = SerializeUtils.unserializeBswabePub(pub_byte, propertyLocation);

		/* read ciphertext */
		tmp = Common.readCpabeData(new ByteArrayInputStream(Base64.getDecoder().decode(encFile)));
		aesBuf = tmp[0];
		cphBuf = tmp[1];
		cph = SerializeUtils.bswabeCphUnserialize(pub, cphBuf);

		share2_byte = Base64.getDecoder().decode(share2);
		privateKeyPart2 = SerializeUtils.unserializeBswabePrvPart2(pub, share2_byte);

		try{
			mDec = SerializeUtils.unserializeBswabeMDec(pub, Base64.getDecoder().decode(mDecFile));
		}catch(Exception e){
			e.printStackTrace();
		}
		var jsonObject = new JsonObject();
		byte[] plt;
		Element m = pub.p.getGT().newElement();
		assert mDec != null;
		m = Abe.dec(pub, privateKeyPart2, cph, mDec).duplicate();
		plt = AESCoder.decrypt(m.toBytes(), aesBuf);

		jsonObject.addProperty("decryptedFile", Base64.getEncoder().encodeToString( plt));
		return jsonObject;
	}

}