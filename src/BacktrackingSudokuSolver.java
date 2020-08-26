/*
 * BacktrackingSudokuSolver is a SudokuSolver
 * which uses recursive backtracking to "guess"
 * values for each of a sudoku puzzle's cells.
 * It may be slow on fresh puzzles,
 * but if at least one solution exists in a puzzle,
 * it is guaranteed to find it.
 * This solver is best used as a last resort
 * when all other solvers are stumped.
 * It does not make any assumptions about
 * the number of solutions that a puzzle given
 * to it has. It also supports puzzles of any size,
 * so long as the puzzle's width is a non-zero
 * perfect square, so 4x4, 9x9, 16x16, etc puzzles
 * are all valid.
 *
 * Pros & Cons of this SudokuSolver Compared to Others:
 * (all clock runtimes provided are based on my own computer and will vary)
 *
 * Pros:
 * - median runtime on a 9x9 is roughly 0.005 seconds
 *      (although average is ~0.97 seconds due to frequency of outliers)
 * - guaranteed to eventually arrive at a solution even on the hardest puzzles
 *      (so long as a solution actually exists)
 * - able so solve puzzles that have multiple solutions
 *
 * Cons:
 * - very prone to extremely slow outlier puzzles, some 9x9's even take 10+ seconds
 * - can take minutes to notice that a 9x9 it was given contained a contradiction
 * - takes hours or even longer to solve most 16x16 puzzles
 * - unable to explain the logic of its solutions to a human
 *
 * Documentation for all inherited methods is found in
 * SudokuSolver.java
 */

import java.io.File;
import java.io.FileNotFoundException;

public class BacktrackingSudokuSolver extends SudokuSolver {
    // board holds values of puzzle in the same configuration
    // you would see them when doing a sudoku yourself on paper
    // some cells may, at any given time, hold the value 0.
    // This means their value has not yet been determined
    // or a working solution for their value has not yet
    // been found
    private int[][] board;

    // the number of cells that it takes to span the puzzle
    // from left to right or top to bottom
    private int width;

    public BacktrackingSudokuSolver() {}

    @Override
    public boolean loadAndSolveSudoku(File puzzleData)
            throws BadSudokuDataFileException, FileNotFoundException {
        loadPuzzleDataFromFile(puzzleData);
        return solutionHunter(0, 0) && validateSolution();
    }

    // param
    // other- SudokuSolver which itself has had loadAndSolveSudoku called on it
    // returns
    // true iff puzzle is able to be solved which will
    // occur iff the puzzle has at least one solution
    // modifies
    // loads board from other and then tries to finish solving it
    // throws
    // IllegalArgumentException if other does not have a
    // board loaded into it i.e. if loadAndSolveSudoku hasn't
    // been called on other yet
    public boolean loadAndSolveSudoku(SudokuSolver other) {
        int otherWidth;
        try {
            otherWidth = other.getWidth();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException();
        }

        // load board from other
        initializeBoard(otherWidth);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                board[i][j] = other.getValue(i, j);
            }
        }

        // attempt to finish solving
        return solutionHunter(0, 0) && validateSolution();
    }

    // returns
    // true iff the puzzle is truly solved
    // this is necessary because solutionHunter
    // can be fooled by certain self-contradicting
    // puzzles
    // modifies
    // if the solution is not valid,
    // modify puzzle state to reflect this
    private boolean validateSolution() {
        // note that this isn't exactly the most efficient
        // implementation of a legal solution check but this
        // will never be the limiting factor on runtime.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                if(!isLegal(i, j)) {
                    // this setting is done so future accesses
                    // will see that the board is unsolved
                    // without this method needing to
                    // be called every time puzzle board is
                    // read from
                    // in short this makes it so
                    // board contains 0 <-> contradiction present
                    board[0][0] = 0;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int getWidth() {
        if (board == null) {
            throw new IllegalStateException();
        }
        return width;
    }

    @Override
    public int getValue(int row, int column) {
        if (board == null) {
            throw new IllegalStateException();
        } else if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }

        if (contradictionWasFound()) {
            return -1;
        }
        return board[row][column];
    }

    @Override
    public boolean contradictionWasFound() {
        if (board == null) {
            return false;
        }

        // thanks to validateSolution and
        // the way recursive backtracking
        // works, it always stands that
        // board contains 0 <-> contradiction present
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                if(board[i][j] == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void initializeBoard(int width) {
        // ensure width is perfect square and non-zero
        double sqrt = Math.sqrt(width);
        if (sqrt != Math.floor(sqrt) || width < 1) {
            throw new IllegalArgumentException();
        }

        this.width = width;
        board = new int[width][width];
    }

    @Override
    protected void setValue(int row, int column, int value) {
        if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }
        if (value < 1 || value > width) {
            throw new IllegalArgumentException();
        }
        board[row][column] = value;
    }

    // params
    // row and column together specify the location of a
    // cell in the puzzle board
    // returns
    // true iff the value located at row, column is in range [1, width]
    // AND does not conflict with any other values in the
    // same row, column, or sector of the board
    // throws
    // IndexOutOfBoundsException if row or column is outside
    // of the bounds of the puzzle board
    private boolean isLegal(int row, int column) {
        if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }

        int value = board[row][column];

        if (value > width || value < 1) {
            return false;
        }

        // check that no value in this column conflicts
        for (int i = 0; i < width; i++) {
            if (board[i][column] == value && i != row) {
                return false;
            }
        }

        // check that no value in this row conflicts
        for (int i = 0; i < width; i++) {
            if (board[row][i] == value && i != column) {
                return false;
            }
        }

        // check that no value in sector conflicts
        int sectorWidth = (int) Math.sqrt(width);
        int verticalSector = row / sectorWidth;
        int horizontalSector = column / sectorWidth;

        for (int i = verticalSector * sectorWidth;
             i < (verticalSector * sectorWidth) + sectorWidth; i++) {
            for (int j = horizontalSector * sectorWidth;
                 j < (horizontalSector * sectorWidth) + sectorWidth; j++) {
                if (board[i][j] == value && !(row == i && column == j)) {
                    return false;
                }
            }
        }
        return true;
    }

    // params
    // row and column together specify the location
    // of the cell on the puzzle board that we are attempting
    // to find a legal value for
    // returns
    // true iff the board was successfully solved,
    // which is true iff the board did not contain
    // a contradiction
    // modifies
    // if the current board is solvable, it will be solved
    // else board will be remembered as contradictory
    private boolean solutionHunter(int row, int column) {
        // if currently past the end, mission accomplished
        if (row == width && column == 0) {
            return true;
        }

        // compute location of next cell to work on
        int nextRow = row;
        int nextColumn = column + 1;
        if(column == width - 1) {
            nextRow = row + 1;
            nextColumn = 0;
        }

        // the below condition is true when the value of
        // board[row][column] was given to us from the start
        if (board[row][column] !=  0) {
            return solutionHunter(nextRow, nextColumn);
        }

        // use recursive backtracking to find
        // board solution for this branch if
        // one exists
        for (int i = 1; i <= width; i++) {
            // choose
            board[row][column] = i;

            // test
            if (isLegal(row, column) && solutionHunter(nextRow, nextColumn)) {
                return true;
            }

            // unchoose
            board[row][column] = 0;
        }

        // this branch is dead
        return false;
    }
}