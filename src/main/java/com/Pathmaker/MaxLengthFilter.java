/*
 * Copyright (c) 2025, JosteinE
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	  list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.Pathmaker;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class MaxLengthFilter extends DocumentFilter
{
    private final int max;

    public MaxLengthFilter(int max) {
        this.max = max;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
            throws BadLocationException {

        // Swing sometimes passes null text during input events
        if (text == null)
            return;

        // Filter out non-printable control characters
        text = filterPrintable(text);

        // Only allow insertion if total length stays within limit
        int currentLength = fb.getDocument().getLength();
        int newLength = currentLength + text.length();

        if (newLength <= max)
        {
            // Entire text can be inserted safely
            super.insertString(fb, offset, text, attrs);
        }
        else
        {
            // Only insert part of the text that fits
            int allowed = max - currentLength;

            if (allowed > 0)
            {
                super.insertString(fb, offset, text.substring(0, allowed), attrs);
            }
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException
    {

        // Replace text can also be null
        if (text == null)
            text = "";

        int currentLength = fb.getDocument().getLength();
        int newLength = currentLength - length + text.length();

        if (newLength <= max)
        {
            // Replacement does not exceed max length
            super.replace(fb, offset, length, text, attrs);
        }
        else
        {
            // Only some of the new text can fit
            int allowed = max - (currentLength - length);
            if (allowed > 0)
            {
                // Insert only the allowed substring, preserving caret & selection
                super.replace(fb, offset, length, text.substring(0, allowed), attrs);
            }
        }
    }


     // remove() is called before text is deleted.
     // Usually no restrictions are needed for removal.

    @Override
    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        super.remove(fb, offset, length);
    }


    // Utility method to strip out non-printable control characters.
    // (Allows letters, digits, punctuation, symbols, emoji, etc.)
    private String filterPrintable(String input) {
        StringBuilder out = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (!Character.isISOControl(c)) {  // filter out control chars
                out.append(c);
            }
        }
        return out.toString();
    }
}