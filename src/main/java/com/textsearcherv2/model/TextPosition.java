package com.textsearcherv2.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextPosition {
    private int lineNumber;
    private int columnNumber;

    @Override
    public String toString() {
        return "[lineOffset=" + lineNumber + ", charOffset=" + columnNumber + "]";
    }


}
