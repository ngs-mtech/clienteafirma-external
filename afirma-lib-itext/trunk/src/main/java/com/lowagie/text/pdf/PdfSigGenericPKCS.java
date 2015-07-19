/*
 * Copyright 2004 by Paulo Soares.
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */
package com.lowagie.text.pdf;

import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.cert.CRL;
import java.security.cert.Certificate;

import com.lowagie.text.ExceptionConverter;

/**
 * A signature dictionary representation for the standard filters.
 */
public abstract class PdfSigGenericPKCS extends PdfSignature {
    /**
     * The hash algorithm, for example "SHA1"
     */
    String hashAlgorithm;
    /**
     * The crypto provider
     */
    String provider = null;
    /**
     * The class instance that calculates the PKCS#1 and PKCS#7
     */
    private PdfPKCS7 pkcs;
    /**
     * The subject name in the signing certificate (the element "CN")
     */
    private String   name;

    private byte externalDigest[];
    private byte externalRSAdata[];
    private String digestEncryptionAlgorithm;

    /**
     * Creates a generic standard filter.
     * @param filter the filter name
     * @param subFilter the sub-filter name
     */
    private PdfSigGenericPKCS(final PdfName filter, final PdfName subFilter) {
        super(filter, subFilter);
    }

    /**
     * Sets the crypto information to sign.
     * @param privKey the private key
     * @param certChain the certificate chain
     * @param crlList the certificate revocation list. It can be <CODE>null</CODE>
     */
    void setSignInfo(final PrivateKey privKey, final Certificate[] certChain, final CRL[] crlList) {
        try {
            this.pkcs = new PdfPKCS7(privKey, certChain, crlList, this.hashAlgorithm, this.provider, PdfName.ADBE_PKCS7_SHA1.equals(get(PdfName.SUBFILTER)));
            this.pkcs.setExternalDigest(this.externalDigest, this.externalRSAdata, this.digestEncryptionAlgorithm);
            if (PdfName.ADBE_X509_RSA_SHA1.equals(get(PdfName.SUBFILTER))) {
                final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (final Certificate element : certChain) {
                    bout.write(element.getEncoded());
                }
                bout.close();
                setCert(bout.toByteArray());
                setContents(this.pkcs.getEncodedPKCS1());
            } else {
				setContents(this.pkcs.getEncodedPKCS7());
			}
            this.name = PdfPKCS7.getSubjectFields(this.pkcs.getSigningCertificate()).getField("CN");
            if (this.name != null) {
				put(PdfName.NAME, new PdfString(this.name, PdfObject.TEXT_UNICODE));
			}
            this.pkcs = new PdfPKCS7(privKey, certChain, crlList, this.hashAlgorithm, this.provider, PdfName.ADBE_PKCS7_SHA1.equals(get(PdfName.SUBFILTER)));
            this.pkcs.setExternalDigest(this.externalDigest, this.externalRSAdata, this.digestEncryptionAlgorithm);
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Sets the digest/signature to an external calculated value.
     * @param digest the digest. This is the actual signature
     * @param RSAdata the extra data that goes into the data tag in PKCS#7
     * @param digestEncryptionAlgorithm the encryption algorithm. It may must be <CODE>null</CODE> if the <CODE>digest</CODE>
     * is also <CODE>null</CODE>. If the <CODE>digest</CODE> is not <CODE>null</CODE>
     * then it may be "RSA" or "DSA"
     */
    void setExternalDigest(final byte digest[], final byte RSAdata[], final String digestEncryptionAlgorithm) {
        this.externalDigest = digest;
        this.externalRSAdata = RSAdata;
        this.digestEncryptionAlgorithm = digestEncryptionAlgorithm;
    }

    /**
     * Gets the subject name in the signing certificate (the element "CN")
     * @return the subject name in the signing certificate (the element "CN")
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the class instance that does the actual signing.
     * @return the class instance that does the actual signing
     */
    public PdfPKCS7 getSigner() {
        return this.pkcs;
    }

    /**
     * Gets the signature content. This can be a PKCS#1 or a PKCS#7. It corresponds to
     * the /Contents key.
     * @return the signature content
     */
    public byte[] getSignerContents() {
        if (PdfName.ADBE_X509_RSA_SHA1.equals(get(PdfName.SUBFILTER))) {
			return this.pkcs.getEncodedPKCS1();
		} else {
			return this.pkcs.getEncodedPKCS7();
		}
    }

    /**
     * Creates a standard filter of the type VeriSign.
     */
    static class VeriSign extends PdfSigGenericPKCS {
        /**
         * The constructor for the default provider.
         */
        public VeriSign() {
            super(PdfName.VERISIGN_PPKVS, PdfName.ADBE_PKCS7_DETACHED);
            this.hashAlgorithm = "MD5";
            put(PdfName.R, new PdfNumber(65537));
        }

        /**
         * The constructor for an explicit provider.
         * @param provider the crypto provider
         */
        VeriSign(final String provider) {
            this();
            this.provider = provider;
        }
    }

    /**
     * Creates a standard filter of the type self signed.
     */
    static class PPKLite extends PdfSigGenericPKCS {
        /**
         * The constructor for the default provider.
         */
        public PPKLite() {
            super(PdfName.ADOBE_PPKLITE, PdfName.ADBE_X509_RSA_SHA1);
            this.hashAlgorithm = "SHA1";
            put(PdfName.R, new PdfNumber(65541));
        }

        /**
         * The constructor for an explicit provider.
         * @param provider the crypto provider
         */
        PPKLite(final String provider) {
            this();
            this.provider = provider;
        }
    }

    /**
     * Creates a standard filter of the type Windows Certificate.
     */
    static class PPKMS extends PdfSigGenericPKCS {
        /**
         * The constructor for the default provider.
         */
        public PPKMS() {
            super(PdfName.ADOBE_PPKMS, PdfName.ADBE_PKCS7_SHA1);
            this.hashAlgorithm = "SHA1";
        }

        /**
         * The constructor for an explicit provider.
         * @param provider the crypto provider
         */
        PPKMS(final String provider) {
            this();
            this.provider = provider;
        }
    }
}
