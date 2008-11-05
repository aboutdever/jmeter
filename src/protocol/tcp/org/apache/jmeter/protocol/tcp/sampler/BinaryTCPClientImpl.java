/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 * TCP Sampler Client implementation which reads and writes binary data.  
 * 
 * Input/Output strings are passed as hex-encoded binary strings.
 *
 */
package org.apache.jmeter.protocol.tcp.sampler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

/**
 * Sample TCPClient implementation
 * 
 */
public class BinaryTCPClientImpl implements TCPClient {
    private static final Logger log = LoggingManager.getLoggerForClass();

    private int eomInt = JMeterUtils.getPropDefault("tcp.BinaryTCPClient.eomByte", 1000); // $NON_NLS-1$

    // End of message byte (defaults to none)
    private byte eomByte = (byte) eomInt; // -128 to +127

    private boolean eomIgnore = eomInt < -128 || eomInt > 127;
    
    public BinaryTCPClientImpl() {
        super();
        if (!eomIgnore) {
            log.info("Using eomByte=" + eomByte);
        }
    }

    /**
     * Convert hex string to binary byte array.
     * 
     * @param s - hex-encoded binary string
     * @return Byte array containing binary representation of input hex-encoded string 
     */
    public static final byte[] hexStringToByteArray(String s) {
        if (s.length() % 2 == 0) {
            char[] sc = s.toCharArray();
            byte[] ba = new byte[sc.length / 2];

            for (int i = 0; i < ba.length; i++) {
                int nibble0 = Character.digit(sc[i * 2], 16);
                int nibble1 = Character.digit(sc[i * 2 + 1], 16);
                ba[i] = (byte) ((nibble0 << 4) | (nibble1));
            }

            return ba;
        } else {
            throw new IllegalArgumentException(
                    "Hex-encoded binary string contains an uneven no. of digits");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jmeter.protocol.tcp.sampler.TCPClient#setupTest()
     */
    public void setupTest() {
        log.info("setuptest");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jmeter.protocol.tcp.sampler.TCPClient#teardownTest()
     */
    public void teardownTest() {
        log.info("teardowntest");

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jmeter.protocol.tcp.sampler.TCPClient#write(java.io.OutputStream
     * , java.lang.String)
     */
    public void write(OutputStream os, String s) {
        try {
            os.write(hexStringToByteArray(s));
            os.flush();
        } catch (IOException e) {
            log.warn("Write error", e);
        }
        log.debug("Wrote: " + s);
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jmeter.protocol.tcp.sampler.TCPClient#write(java.io.OutputStream
     * , java.io.InputStream)
     */
    public void write(OutputStream os, InputStream is) {
        throw new UnsupportedOperationException(
                "Method not supported for Length-Prefixed data.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jmeter.protocol.tcp.sampler.TCPClient#read(java.io.InputStream
     * )
     */
    public String read(InputStream is) {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream w = new ByteArrayOutputStream();
        int x = 0;
        try {
            while ((x = is.read(buffer)) > -1) {
                w.write(buffer, 0, x);
                if (!eomIgnore && (buffer[x - 1] == eomByte)) {
                    break;
                }
            }
            /*
             * Timeout is reported as follows: JDK1.3: InterruptedIOException
             * JDK1.4: SocketTimeoutException, which extends
             * InterruptedIOException
             *
             * So to make the code work on both, just check for
             * InterruptedIOException
             *
             * If 1.3 support is dropped, can change to using
             * SocketTimeoutException
             *
             * For more accurate detection of timeouts under 1.3, one could
             * perhaps examine the Exception message text...
             *
             */
        } catch (SocketTimeoutException e) {
            // drop out to handle buffer
        } catch (InterruptedIOException e) {
            // drop out to handle buffer
        } catch (IOException e) {
            log.warn("Read error:" + e);
            return "";
        }

        // do we need to close byte array (or flush it?)
        log.debug("Read: " + w.size() + "\n" + w.toString());
        return JOrphanUtils.baToHexString(w.toByteArray());
    }

    /**
     * @return Returns the eomByte.
     */
    public byte getEolByte() {
        return eomByte;
    }

    /**
     * @param eomByte
     *            The eomByte to set.
     */
    public void setEolByte(byte eomByte) {
        this.eomByte = eomByte;
        eomIgnore = false;
    }
}
