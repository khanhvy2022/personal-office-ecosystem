package org.libreoffice.androidlib.lok;

import java.io.Serializable;

import androidx.annotation.Keep;

@Keep
public class LokClipboardEntry implements Serializable {
    public String mime;
    public byte[] data;
}
