/*
 * SudokuSolver is an abstract class that
 * is meant to be extended by any class that
 * has some persistent internal representation
 * of a sudoku puzzle and also has the capacity
 * to solve, or attempt to solve such puzzles.
 * SudokuSolver supports puzzles of any size,
 * so long as the puzzle's width is a non-zero
 * perfect square, so 4x4, 9x9, 16x16, etc puzzles
 * are all supported.
 *
 * SudokuSolvers also have the ability to read
 * in puzzle data from utf-8 text files, which
 * hereafter will be referred to as sdku files
 * as that is the extension I use to denote them.
 * sdku files contain the following components in order:
 *
 * 1. Optional Header Comment
 *   An sdku file can begin with arbitrarily many lines
 *   of "comments". A comment is any line which begins with
 *   with two forward slashes, much like single-line java comments.
 *   After the first non-comment line, however, no more comment
 *   lines are permitted.
 * 2. Puzzle Width Specifier
 *   The next line of the sdku file (or first if there are no comments)
 *   must be a single integer representing the width of the puzzle.
 *   The width of a puzzle is the number of cells it takes
 *   to span the puzzle from left to right or top to bottom.
 *   This width must be a non-zero perfect square for reasons which
 *   become clear when considering the function of individual
 *   sectors of the puzzle.
 * 3. Cell Values
 *   If the width specifier is some value n, the rest of the sdku
 *   file must be exactly n lines long where each line contains
 *   exactly n integer tokens with no trailing spaces. Each of these tokens
 *   represents the value of the corresponding cell of the puzzle.
 *   Any cell whose value is not given by the puzzle will use 0
 *   as a stand in for the unknown value.
 * Below is an example 4 x 4 puzzle sdku file corresponding to this puzzle:
 * https://www.sudokuweb.org/wp-content/uploads/2013/04/sudoku-kids-4x4-10.png
 *
 * // This is a header comment
 * // for an example sdku file
 * 4
 * 0 3 4 0
 * 4 0 0 2
 * 1 0 0 3
 * 0 2 1 0
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Scanner;

public abstract class SudokuSolver {

    // param
    // puzzleData- sdku file containing width and content of puzzle
    // returns
    // true if the puzzle was successfully solved
    // false if the solver couldn't solve the puzzle or if no solution exists
    // modifies
    // the puzzle data is loaded into the solver and an attempt is made to solve it.
    // after this, it is possible to access the data in the (solved?) puzzle
    // using any public read methods below
    // throws
    // FileNotFoundException if file cannot be found
    // BadSudokuDataFileException if something is wrong with
    // sdku file's formatting, it isn't readable, or it is a directory
    public abstract boolean loadAndSolveSudoku(File puzzleData)
            throws BadSudokuDataFileException, FileNotFoundException;

    // params
    // row and column together specify a cell in the puzzle
    // returns
    // specified cell's solved value, 0 if the value has yet to
    // be determined, or
    // if it has been found that the puzzle contains a
    // contradiction i.e. if supplied a puzzle with no solution
    // -1 is returned for any in-bounds row column pair
    // throws
    // IndexOutOfBoundsException if row or column is out of range [0, width)
    // IllegalStateException if no sudoku data has been loaded yet
    public abstract int getValue(int row, int column);

    // returns
    // the number of cells that the puzzle
    // spans left to right or equivalently top to bottom
    // throws
    // IllegalStateException iff no sudoku data has been loaded
    public abstract int getWidth();

    // returns
    // true iff puzzle data has been loaded and it was found that the provided
    // puzzle contained a contradiction,
    // equivalently returns true iff puzzle is found to have no valid solution
    public abstract boolean contradictionWasFound();

    // params
    // row and column together specify the location of a cell
    // in the puzzle board
    // value is the value that the cell will be set to
    // modifies
    // the puzzle board such that specified cell is set to given value
    // throws
    // IndexOutOfBoundsException if row or column is not in range [0, width)
    // IllegalArgumentException if value is not in range [1, width]
    protected abstract void setValue(int row, int column, int value);

    // params
    // width of puzzle board that will need to be represented
    // modifies
    // causes this particular solver to initialize whatever
    // structures it may need to in order to accommodate
    // a puzzle with the given width
    // throws
    // IllegalArgumentException iff width < 1 or width is not a perfect square
    protected abstract void initializeBoard(int width);

    // params
    // row and column together specify the location of a cell
    // in the puzzle
    // returns true iff that cell is in bounds for the puzzle
    protected boolean isInBounds(int row, int column) {
        int width = getWidth();
        return row >= 0 && row < width && column >= 0 && column < width;
    }

    // returns
    // string representation of puzzle board
    // any cell whose value has not yet been
    // determined will be shown empty
    // if it has been found that the puzzle contains
    // a contradiction, the board's cells will all
    // display value -1
    // if no sudoku data has yet been loaded,
    // returns "No puzzle data has been loaded into solver.\n"
    @Override
    public String toString() {
        try {
            getValue(0,0);
        } catch (IllegalStateException e) {
            return "No puzzle data has been loaded into solver.\n";
        }

        // used to provide appropriately-sized padding
        // to each cell so rows make an even line
        int maxLength = String.valueOf(getWidth()).length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getWidth(); i++) {
            for (int j = 0; j < getWidth(); j++) {

                // add this cell's representation to string builder
                sb.append("|");
                // determine what to display for cell's value
                int curValue = getValue(i, j);
                String cellContent;
                if (curValue == 0) {  // it's 0 so we display an empty cell
                    cellContent = "";
                } else {
                    cellContent = String.valueOf(curValue);
                }

                // apply padding, an extra space is added because it looks
                // better that way
                sb.append(" ".repeat(Math.max(0, maxLength - cellContent.length() + 1)));
                sb.append(cellContent);
            }

            // fencepost nicely
            sb.append("|");
            if (i != getWidth() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    // param
    // puzzleData- sdku file that puzzle board data will be loaded from
    // modifies
    // initializes solver's puzzle board to size specified in file and
    // loads all puzzle data into solver from the file
    // throws
    // BadSudokuDataFileException if provided file is not interpretable
    // as a valid sdku file
    // FileNotFoundException if puzzleData cannot be found, is a directory,
    // or cannot be read
    protected final void loadPuzzleDataFromFile(File puzzleData)
            throws BadSudokuDataFileException, FileNotFoundException {

        // create scanner while implicitly testing that file is present,
        // readable, and non-directory
        Scanner dataScanner = new Scanner(puzzleData);

        if (!dataScanner.hasNextLine()) {
            throw new BadSudokuDataFileException(puzzleData.getName(),
                    "File is empty or not does conform to utf-8 encoding");
        }

        // consume any comment lines
        String curLine = dataScanner.nextLine();
        while(curLine.startsWith("//") && dataScanner.hasNextLine()) {
            curLine = dataScanner.nextLine();
        }

        // ensure the file contains something other than comments
        if (!dataScanner.hasNextLine()) {
            throw new BadSudokuDataFileException(puzzleData.getName(),
                    "Provided file contains no puzzle data");
        }

        int width;

        // read width specifier from file, ensuring
        // that it is an int
        try  {
            width = Integer.parseInt(curLine);
        } catch (NumberFormatException e) {
            throw new BadSudokuDataFileException(puzzleData.getName(),
                    "First substantive line " +
                    "of file is not single integer indicating\n\t" +
                    "width of puzzle");
        }

        // try to initialize puzzle board to size width,
        // making sure width specifier isn't bad
        try {
            initializeBoard(width);
        } catch (IllegalArgumentException e) {
            throw new BadSudokuDataFileException(puzzleData.getName(), "Illegal " +
                    "width specifier");
        }

        // for each line of cell values in file
        // verify that it is properly formatted and then
        // assign the specified values to the cells of
        // the sudoku board, also verify that the
        // appropriate number of lines is present
        for (int i = 0; i < width; i++) {
            if (!dataScanner.hasNext()) {
                throw new BadSudokuDataFileException(puzzleData.getName(),
                        "Provided file is shorter than expected");
            }

            Scanner tokenScanner = new Scanner(dataScanner.nextLine());
            // for each token in line of text, verify that it is an integer
            // in the appropriate range of values, and that there are the
            // correct number of values present in the line
            for (int j = 0; j < width; j++) {
                if (!tokenScanner.hasNext()) {
                    throw new BadSudokuDataFileException(puzzleData.getName(),
                            "Row " + i + " of cell values has fewer elements than expected");
                }

                // check that next value is an integer in appropriate range
                int next = 0;
                try {
                    next = tokenScanner.nextInt();
                    if (next != 0) {
                        setValue(i, j, next);
                    }
                } catch (InputMismatchException e) {
                    throw new BadSudokuDataFileException(puzzleData.getName(),
                            "Non-integer found where cell value was expected ");
                } catch (IllegalArgumentException e) {
                    throw new BadSudokuDataFileException(puzzleData.getName(),
                            "The value \"" + next + "\" in row " + i + " of puzzle is " +
                                    "outside of legal cell " +
                                    "value range of 1 to " + width);
                }
            }

            // the last token should have already been processed
            if (tokenScanner.hasNextLine()) {
                throw new BadSudokuDataFileException(puzzleData.getName(),
                        "Row " + i + " of cell values has more elements than expected");
            }
        }

        // the last line in file should have already been processed
        if(dataScanner.hasNextLine()) {
            throw new BadSudokuDataFileException(puzzleData.getName(),
                    "File is longer than expected");
        }
    }

    // checked exception that is meant to be thrown when a user
    // attempts to load a bad sdku file
    public static class BadSudokuDataFileException extends Exception {
        private final static String errorMessage =
                "\n\tInvalid formatting of sudoku data";

        // default constructor
        private BadSudokuDataFileException() {
            super(errorMessage);
        }

        // allows for extra information in exception
        private BadSudokuDataFileException
                (String filename, String additionalInformation) {
            super(errorMessage + " in \"" + filename +
                    "\"\n\t" + additionalInformation);
        }
    }
}