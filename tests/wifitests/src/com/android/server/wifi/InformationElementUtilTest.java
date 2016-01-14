/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.server.wifi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.net.wifi.ScanResult.InformationElement;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.server.wifi.util.InformationElementUtil;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Unit tests for {@link com.android.server.wifi.util.InformationElementUtil}.
 */
@SmallTest
public class InformationElementUtilTest {

    // SSID Information Element tags
    private static final byte[] TEST_SSID_BYTES_TAG = new byte[] { (byte) 0x00, (byte) 0x0B };
    // SSID Information Element entry used for testing.
    private static final byte[] TEST_SSID_BYTES = "GoogleGuest".getBytes();
    // Valid zero length tag.
    private static final byte[] TEST_VALID_ZERO_LENGTH_TAG =
            new byte[] { (byte) 0x0B, (byte) 0x00 };
    // BSS_LOAD Information Element entry used for testing.
    private static final byte[] TEST_BSS_LOAD_BYTES_IE =
            new byte[] { (byte) 0x0B, (byte) 0x01, (byte) 0x08 };

    /*
     * Function to provide SSID Information Element (SSID = "GoogleGuest").
     *
     * @return byte[] Byte array representing the test SSID
     */
    private byte[] getTestSsidIEBytes() throws IOException {
        return concatenateByteArrays(TEST_SSID_BYTES_TAG, TEST_SSID_BYTES);
    }

    /*
     * Function used to set byte arrays used for testing.
     *
     * @param byteArrays variable number of byte arrays to concatenate
     * @return byte[] Byte array resulting from concatenating the arrays passed to the function
     */
    private static byte[] concatenateByteArrays(byte[]... byteArrays) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] b : byteArrays) {
            baos.write(b);
        }
        baos.flush();
        return baos.toByteArray();
    }

    /**
     * Test parseInformationElements with an empty byte array.
     * Expect parseInformationElement to return an empty InformationElement array.
     */
    @Test
    public void parseInformationElements_withEmptyByteArray() {
        byte[] emptyBytes = new byte[0];
        InformationElement[] results =
                InformationElementUtil.parseInformationElements(emptyBytes);
        assertEquals("parsed results should be empty", 0, results.length);
    }

    /**
     * Test parseInformationElements called with a null parameter.
     * Expect parseInfomrationElement to return an empty InformationElement array.
     */
    @Test
    public void parseInformationElements_withNullBytes() {
        byte[] nullBytes = null;
        InformationElement[] results =
                InformationElementUtil.parseInformationElements(nullBytes);
        assertEquals("parsed results should be empty", 0, results.length);
    }

    /*
     * Test parseInformationElements with a single element represented in the byte array.
     * Expect a single element to be returned in the InformationElements array.  The
     * length of this array should be 1 and the contents should be valid.
     *
     * @throws java.io.IOException
     */
    @Test
    public void parseInformationElements_withSingleElement() throws IOException {
        byte[] ssidBytes = getTestSsidIEBytes();

        InformationElement[] results =
                InformationElementUtil.parseInformationElements(ssidBytes);
        assertEquals("Parsed results should have 1 IE", 1, results.length);
        assertEquals("Parsed result should be a ssid", InformationElement.EID_SSID, results[0].id);
        assertArrayEquals("parsed SSID does not match input",
                TEST_SSID_BYTES, results[0].bytes);
    }

    /*
     * Test parseInformationElement with extra padding in the data to parse.
     * Expect the function to return the SSID information element.
     *
     * Note: Experience shows that APs often pad messages with 0x00.  This happens to be the tag for
     * EID_SSID.  This test checks if padding will be properly discarded.
     *
     * @throws java.io.IOException
     */
    @Test
    public void parseInformationElements_withExtraPadding() throws IOException {
        byte[] paddingBytes = new byte[10];
        Arrays.fill(paddingBytes, (byte) 0x00);
        byte[] ssidBytesWithPadding = concatenateByteArrays(getTestSsidIEBytes(), paddingBytes);

        InformationElement[] results =
                InformationElementUtil.parseInformationElements(ssidBytesWithPadding);
        assertEquals("Parsed results should have 1 IE", 1, results.length);
        assertEquals("Parsed result should be a ssid", InformationElement.EID_SSID, results[0].id);
        assertArrayEquals("parsed SSID does not match input",
                TEST_SSID_BYTES, results[0].bytes);
    }

    /*
     * Test parseInformationElement with two elements where the second element has an invalid
     * length.
     * Expect the function to return the first valid entry and skip the remaining information.
     *
     * Note:  This test partially exposes issues with blindly parsing the data.  A higher level
     * function to validate the parsed data may be added.
     *
     * @throws java.io.IOException
     * */
    @Test
    public void parseInformationElements_secondElementInvalidLength() throws IOException {
        byte[] invalidTag = new byte[] { (byte) 0x01, (byte) 0x08, (byte) 0x08 };
        byte[] twoTagsSecondInvalidBytes = concatenateByteArrays(getTestSsidIEBytes(), invalidTag);

        InformationElement[] results =
                InformationElementUtil.parseInformationElements(twoTagsSecondInvalidBytes);
        assertEquals("Parsed results should have 1 IE", 1, results.length);
        assertEquals("Parsed result should be a ssid.", InformationElement.EID_SSID, results[0].id);
        assertArrayEquals("parsed SSID does not match input",
                TEST_SSID_BYTES, results[0].bytes);
    }

    /*
     * Test parseInformationElements with two valid Information Element entries.
     * Expect the function to return an InformationElement array with two entries containing valid
     * data.
     *
     * @throws java.io.IOException
     */
    @Test
    public void parseInformationElements_twoElements() throws IOException {
        byte[] twoValidTagsBytes =
                concatenateByteArrays(getTestSsidIEBytes(), TEST_BSS_LOAD_BYTES_IE);

        InformationElement[] results =
                InformationElementUtil.parseInformationElements(twoValidTagsBytes);
        assertEquals("parsed results should have 2 elements", 2, results.length);
        assertEquals("First parsed element should be a ssid",
                InformationElement.EID_SSID, results[0].id);
        assertArrayEquals("parsed SSID does not match input",
                TEST_SSID_BYTES, results[0].bytes);
        assertEquals("second element should be a BSS_LOAD tag",
                InformationElement.EID_BSS_LOAD, results[1].id);
        assertEquals("second element should have data of length 1", 1, results[1].bytes.length);
        assertEquals("second element data was not parsed correctly.",
                (byte) 0x08, results[1].bytes[0]);
    }

    /*
     * Test parseInformationElements with two elements where the first information element has a
     * length of zero.
     * Expect the function to return an InformationElement array with two entries containing valid
     * data.
     *
     * @throws java.io.IOException
     */
    @Test
    public void parseInformationElements_firstElementZeroLength() throws IOException {
        byte[] zeroLengthTagWithSSIDBytes =
                concatenateByteArrays(TEST_VALID_ZERO_LENGTH_TAG, getTestSsidIEBytes());

        InformationElement[] results =
                InformationElementUtil.parseInformationElements(zeroLengthTagWithSSIDBytes);
        assertEquals("Parsed results should have 2 elements.", 2, results.length);
        assertEquals("First element tag should be EID_BSS_LOAD",
                InformationElement.EID_BSS_LOAD, results[0].id);
        assertEquals("First element should be length 0", 0, results[0].bytes.length);

        assertEquals("Second element should be a ssid", InformationElement.EID_SSID, results[1].id);
        assertArrayEquals("parsed SSID does not match input",
                TEST_SSID_BYTES, results[1].bytes);
    }

    /*
     * Test parseInformationElements with two elements where the first element has an invalid
     * length.  The invalid length in the first element causes us to miss the start of the second
     * Infomation Element.  This results in a single element in the returned array.
     * Expect the function to return a single entry in an InformationElement array. This returned
     * entry is not validated at this time and does not contain valid data (since the incorrect
     * length was used).
     * TODO: attempt to validate the data and recover as much as possible.  When the follow-on CL
     * is in development, this test will be updated to reflect the change.
     *
     * @throws java.io.IOException
     */
    @Test
    public void parseInformationElements_firstElementWrongLength() throws IOException {
        byte[] invalidLengthTag = new byte[] {(byte) 0x0B, (byte) 0x01 };
        byte[] invalidLengthTagWithSSIDBytes =
                concatenateByteArrays(invalidLengthTag, getTestSsidIEBytes());

        InformationElement[] results =
                InformationElementUtil.parseInformationElements(invalidLengthTagWithSSIDBytes);
        assertEquals("Parsed results should have 1 element", 1, results.length);
        assertEquals("First result should be a EID_BSS_LOAD tag.",
                InformationElement.EID_BSS_LOAD, results[0].id);
        assertEquals("First result should have data of 1 byte", 1, results[0].bytes.length);
        assertEquals("First result should have data set to 0x00",
                invalidLengthTagWithSSIDBytes[2], results[0].bytes[0]);
    }
}