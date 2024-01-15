package com.textsearcherv2.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * This class represents the position of a text in terms of line number and column number.
 *
 * <p>
 * This class provides methods to get and set the line number and column number, as well as a toString() method
 * to represent the position in the format "[lineOffset=<line number>, charOffset=<column number>]".
 * </p>
 */
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
