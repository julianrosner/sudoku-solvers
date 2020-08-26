/*
 * DeductiveSudokuSolver is a SudokuSolver which uses logical
 * reasoning to solve sudoku puzzles in much the same way as a human would.
 * The sudoku solving techniques in its repertoire
 * are identifying hidden singles, naked sets of any
 * size, and implicitly noticing when a cell only has
 * one legal value remaining. It also expects
 * (as almost everyone in the sudoku community does)
 * that any puzzle it's given has exactly one solution.
 * If given a multiple-solution puzzle, this solver will
 * NOT put incorrect values into any cells, but it also
 * will not be able to fully solve such a puzzle.
 * It also supports puzzles of any size,
 * so long as the puzzle's width is a non-zero
 * perfect square, so 4x4, 9x9, 16x16, etc puzzles
 * are all valid.
 *
 * This class was originally conceived as a complement
 * to BacktrackingSudokuSolver to soften puzzles up
 * before the backtracking begins, so it prioritizes speed
 * and as such does not implement techniques that are either
 * intensely time-consuming or rarely-useful.
 * Although, in practice I've found this class to still be quite clever.
 *
 * Pros & Cons of this SudokuSolver Compared to Others:
 * (all clock runtimes provided are based on my own computer and will vary)
 *
 * Pros:
 * - median runtime on a 9x9 is roughly 0.01 seconds
 * - significantly slower outliers of any dimension are virtually non-existent (as far I can tell)
 *      no 9x9 I've found has taken longer that 0.06 seconds
 * - solutions can be explained logically to humans
 * - contradictory puzzles take no longer to process than others
 * - no measurable difference in how long solving a 16x16 takes compared to a 9x9
 * - capable of solving any puzzle I could find in periodicals or magazines
 * - able to determine very quickly if it isn't smart enough to solve a puzzle
 *
 * Cons:
 * - cannot solve puzzles that have multiple solutions
 * - some single-solution puzzles are just too hard for this implementation to solve
 *      (although these are quite rare in practice)
 *
 * To learn more about the above jargon visit
 * https://www.sudokuwiki.org/sudoku.htm
 * this jargon will be used several times
 * throughout the rest of this documentation
 *
 * Documentation for all inherited methods is found in
 * SudokuSolver.java
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class DeductiveSudokuSolver extends SudokuSolver {
    // The scratchMarks field is a three
    // dimensional array where the first two-coordinates
    // specify the row and column of a cell in the puzzle
    // and the third coordinate specifies a particular scratch mark in that cell
    // (0 indexed so scratchMarks[row][column][0] corresponds to the
    // scratch mark for value 1 in the cell at location (row, column)).
    // Scratch marks are the set of values which have not yet been
    // ruled out as the true value for a given cell. When only one
    // scratch mark remains for a cell, only one value is possible, and
    // this is known to be the cell's true value.
    // scratchMarks is this implementation's representation of the puzzle
    // board and it is referred to as such when the mental schema of a
    // standard sudoku puzzle board is more appropriate for the situation than
    // the image of a three dimensional array of booleans.
    private boolean[][][] scratchMarks;

    // the number of cells that it takes to span the puzzle
    // from left to right or top to bottom
    private int width;

    public DeductiveSudokuSolver() {}

    @Override
    public boolean loadAndSolveSudoku(File puzzleData)
            throws BadSudokuDataFileException, FileNotFoundException {
        loadPuzzleDataFromFile(puzzleData);
        int preIterationScratchMarkCount = scratchMarkCount() + 1;

        // repeat solving algorithms until either no progress
        // is made for an entire loop iteration or the
        // puzzle is solved
        while (scratchMarkCount() < preIterationScratchMarkCount
                && scratchMarkCount() != width * width
                && !contradictionWasFound()) {

            preIterationScratchMarkCount = scratchMarkCount();
            allHiddenSingles();

            // use the more time intensive operations
            // only if we haven't made any progress and the puzzle
            // isn't already solved
            if(scratchMarkCount() == preIterationScratchMarkCount
                    && scratchMarkCount() != width * width
                    && !contradictionWasFound()) {
                allNakedSets(width);
            }
        }
        return scratchMarkCount() == width * width
                && !contradictionWasFound();
    }

    @Override
    public int getWidth() {
        if (scratchMarks == null) {
            throw new IllegalStateException();
        }
        return width;
    }

    @Override
    public int getValue(int row, int column) {
        if (scratchMarks == null) {
            throw new IllegalStateException();
        } else if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }

        // this upholds promise of SudokuSolver
        if(contradictionWasFound()) {
            return -1;
        }

        return internalGetValue(row, column);
    }

    // params
    // row and column together specify the location of a
    // cell in the puzzle
    // returns
    // the value of specified cell, 0 if the value is
    // yet to be determined, or if the puzzle contains a
    // contradiction, may or may not return -1
    private int internalGetValue(int row, int column) {
        int ret = -1;
        // a cell's value is considered to be determined
        // when exactly one scratch mark is present
        for (int i = 1; i <= width; i++) {
            if (isPossible(row, column, i)) {
                if (ret != -1) {
                    // at least 2 values are possible for this cell
                    // so it is not determined
                    return 0;
                } else {
                    ret = i;
                }
            }
        }
        return ret;
    }

    @Override
    public boolean contradictionWasFound() {
        if (scratchMarks == null) {
            return false;
        }

        // -1 present in puzzle board <-> contradiction found
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                if(internalGetValue(i, j) < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // returns
    // number of scratch marks remaining in scratchMarks
    // throws
    // IllegalStateException if loadAndSolveSudoku
    // has not yet been called
    public int scratchMarkCount() {
        if (scratchMarks == null) {
            throw new IllegalStateException();
        }
        int sum = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                for (int value = 1; value <= width; value++) {
                    if (isPossible(i, j, value)) {
                        sum++;
                    }
                }
            }
        }
        return sum;
    }

    // returns
    // true if cell at position (row, column)
    // is believed to be able to assume the value value,
    // that is iff value is in its scratch marks
    // throws
    // IndexOutOfBoundsException
    // if row or column is not in the range [0, width)
    // IllegalArgumentException
    // iff value is not in range [1, width]
    public boolean isPossible(int row, int column, int value) {
        if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }
        if (value < 1 || value > width) {
            throw new IllegalArgumentException();
        }
        return scratchMarks[row][column][value - 1];
    }

    @Override
    protected void initializeBoard(int width) {
        // ensure width is perfect square and non-zero
        double sqrt = Math.sqrt(width);
        if (sqrt != Math.floor(sqrt) || width < 1) {
            throw new IllegalArgumentException();
        }

        this.width = width;
        scratchMarks = new boolean[width][width][width];

        // mark all scratch marks present
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < width; k++) {
                    scratchMarks[i][j][k] = true;
                }
            }
        }
    }

    // modifies
    // if board currently has any hidden singles,
    // they will be taken note of and scratchMarks
    // will be updated to reflect this
    private void allHiddenSingles() {
        hiddenSinglesRowColumn();
        hiddenSinglesSector();
    }

    // param
    // maxN is the largest size of naked set that will be
    // checked for
    // modifies
    // if board currently has any naked sets of size <= maxN,
    // they will be taken note of and scratchMarks
    // will be updated to reflect this
    private void allNakedSets(int maxN) {
        // for every possible sub set of the integers in range [1, width]...
        for (int setIndex = 1; setIndex < Math.pow(2, width); setIndex++) {
            Set<Integer> superSet = indexToSet(setIndex);

            // if the set is in appropriate size range...
            // (we don't learn anything from naked sets of size 1 or size width)
            if (superSet.size() > 1 && superSet.size() < width
                    && superSet.size() < maxN) {

                // check if that set is the super set of
                // values for a naked set of cells on the board
                // and if so update scratchMarks appropriately
                nakedSetsRowColumn(superSet);
                nakedSetsSector(superSet);
            }
        }
    }

    // if hidden singles are present in any rows or column,
    // take note of them and update scratch marks
    // accordingly
    private void hiddenSinglesRowColumn() {
        // for every value...
        for (int value = 1; value <= width; value++) {
            // for every row and column...
            for (int rowOrColumn = 0; rowOrColumn  < width; rowOrColumn++) {
                // find all occurrences of value scratch mark in column
                List<Integer> occurrencesColumn = occurrencesInColumn(rowOrColumn, value);

                // find all occurrences of value scratch mark in row
                List<Integer> occurrencesRow = occurrencesInRow(rowOrColumn, value);
                // if only one cell in current row/column can assume current value
                // set that cell to that value
                if(occurrencesColumn.size() == 1) {
                    setValue(occurrencesColumn.get(0), rowOrColumn, value);
                }
                if (occurrencesRow.size() == 1) {
                    setValue(rowOrColumn, occurrencesRow.get(0), value);
                }
            }
        }
    }

    // if hidden singles are present in any sectors,
    // take note of them and update scratch marks
    // accordingly
    private void hiddenSinglesSector() {
        int sectorsPerSide = (int) Math.sqrt(width);
        // for every value a cell can assume...
        for (int value = 1; value <= width; value++) {
            // for every sector on the board...
            for (int i = 0; i < sectorsPerSide ; i++) {
                for (int j = 0; j < sectorsPerSide; j++) {
                    // if only one cell in current sector can assume current value
                    // set it to that value
                    List<CellLocation> occurrences = occurrencesInSector(i, j, value);
                    if(occurrences.size() == 1) {
                        setValue(occurrences.get(0).row, occurrences.get(0).column, value);
                    }
                }
            }
        }
    }

    // params
    // rowIndex- index of row in question
    // 0 for top and (width - 1) for bottom
    // value- value for which occurrence information is desired
    // returns
    // list of columns for which isPossible(rowIndex, column, value)
    private List<Integer> occurrencesInRow(int rowIndex, int value) {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = 0; i < width; i++) {
            if (isPossible(rowIndex, i, value)) {
                ret.add(i);
            }
        }
        return ret;
    }

    // identical to occurrencesInRow except this one returns
    // the list of rows for which isPossible(row, columnIndex, value)
    private List<Integer> occurrencesInColumn(int columnIndex, int value) {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = 0; i < width; i++) {
            if (isPossible(i, columnIndex, value)) {
                ret.add(i);
            }
        }
        return ret;
    }

    // params
    // sectorRow and SectorColumn are NOT indices directly into board
    // instead they together specify which sector is being looked at
    // where a sector is a group cells with dimensions sqrt(width) x sqrt(width)
    // ie. if you were to iterate through the sectors of
    // a standard 9 x 9 puzzles in reading order the SectorRow, SectorColumn
    // pairs would be (0, 0), (0, 1), (0, 2), (1, 0),
    // (1, 1), (1, 2), (2, 0), (2, 1), (2, 2)
    //
    // occurrencesInSector's behavior is identical to occurrencesInRow except this one returns
    // a list of CellLocations with fields row and column
    // such that isPossible(row, column, value) where (row, column) is in the given sector
    private List<CellLocation> occurrencesInSector(int sectorRow, int sectorColumn, int value) {
        List<CellLocation> ret = new ArrayList<CellLocation>();
        int sectorWidth = (int) Math.sqrt(width);
        for (int i = sectorRow * sectorWidth;
             i < (sectorRow * sectorWidth) + sectorWidth; i++) {
            for (int j = sectorColumn * sectorWidth;
                 j < (sectorColumn * sectorWidth) + sectorWidth; j++) {
                if (isPossible(i, j, value)) {
                    ret.add(new CellLocation(i, j));
                }
            }
        }
        return ret;
    }

    // param
    // superSet, set of scratch mark values we want to check for a naked set under
    // modifies
    // if a naked set of any size exists in any rows or columns under the values in superSet
    // update scratch marks to reflect this
    private void nakedSetsRowColumn(Set<Integer> superSet) {
        // for every row/column...
        for (int coordinateA = 0; coordinateA < width; coordinateA++) {

            // create two sets of candidates, where each element in a set represents
            // the location of a cell whose scratch marks
            // are a subset of superSet
            Set<Integer> nakedCandidates1 = new TreeSet<Integer>();
            Set<Integer> nakedCandidates2 = new TreeSet<Integer>();

            // populate these candidate sets appropriately
            for (int coordinateB = 0; coordinateB < width; coordinateB++) {
                if (cellAbidesSuperset(coordinateB, coordinateA, superSet)) {
                    nakedCandidates1.add(coordinateB);
                }

                if (cellAbidesSuperset(coordinateA, coordinateB, superSet)) {
                    nakedCandidates2.add(coordinateB);
                }
            }

            // if the number of candidates in a set is equal to the size of the
            // superSet, we've found a naked set and must update
            // the scratch marks
            if (nakedCandidates1.size() == superSet.size()) {
                // if a cell isn't part of the naked set,
                // inform it that it cannot hold any of the values
                // in superSet
                for (int i = 0; i < width; i++) {
                    if (!nakedCandidates1.contains(i)) {
                        for (Integer j : superSet) {
                            setImpossible(i, coordinateA, j);
                        }
                    }
                }
            }

            // repeat for rows
            if (nakedCandidates2.size() == superSet.size()) {
                for (int i = 0; i < width; i++) {
                    if (!nakedCandidates2.contains(i)) {
                        for (Integer j: superSet) {
                            setImpossible(coordinateA, i, j);
                        }
                    }
                }
            }
        }
    }

    // behaves identically to nakedSetsRowColumn except
    // it operates on sectors instead of rows and columns
    private void nakedSetsSector(Set<Integer> superSet) {
        // note that the below two ints are always equal
        // but their use, I feel, is more intuitive under
        // the two names for their two uses
        int sectorsPerSide = (int) Math.sqrt(width);
        int sectorWidth = sectorsPerSide;
        // for every sector in the puzzle...
        for (int sectorRow = 0; sectorRow < sectorsPerSide ; sectorRow++) {
            for (int sectorColumn = 0; sectorColumn < sectorsPerSide; sectorColumn++) {

                // explore every cell in that sector...
                // building a set of candidates, where every candidate
                // represents the location of a cell,
                // the scratch marks of which are a subset of superSet
                Set<CellLocation> nakedCandidates = new TreeSet<CellLocation>();
                for (int row = sectorRow * sectorWidth;
                     row < (sectorRow * sectorWidth) + sectorWidth; row++) {
                    for (int column = sectorColumn * sectorWidth;
                         column < (sectorColumn * sectorWidth) + sectorWidth; column++) {
                        if (cellAbidesSuperset(row, column, superSet)) {
                            nakedCandidates.add(new CellLocation(row, column));
                        }
                    }
                }

                // if the number of discovered candidates is equal to the number
                // of elements in the superSet,
                // then we indeed have found a naked set and must inform
                // all non-members of said set in this sector that they cannot
                // possibly evaluate to any of the values in the superset cell's
                // set of scratch marks
                if (nakedCandidates.size() == superSet.size()) {

                    // re-traverse sector removing appropriate scratch marks
                    for (int row = sectorRow * sectorWidth;
                         row < (sectorRow * sectorWidth) + sectorWidth; row++) {
                        for (int column = sectorColumn * sectorWidth;
                             column < (sectorColumn * sectorWidth) + sectorWidth; column++) {
                            if (!nakedCandidates.contains(new CellLocation (row, column))) {
                                for (Integer i : superSet) {
                                    setImpossible(row, column, i);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // param
    // setIndex represents the index of the desired set
    // returns
    // a subset of the integers in range [1, width],
    // which one of these sets is returned is not guaranteed, only that
    // if called with every value in range
    // [1, 2 ^ width), every subset of
    // integers in range [1, width] is returned exactly once
    private Set<Integer> indexToSet(int setIndex) {
        Set<Integer> retSet = new TreeSet<Integer>();
        for (int i = 0; i < width; i++) {
            if ((setIndex & (1 << i)) != 0 ) {
                retSet.add(i + 1);
            }
        }
        return retSet;
    }

    // returns true iff the scratch marks of the cell at (row, column)
    // are a subset of superSet
    private boolean cellAbidesSuperset(int row, int column, Set<Integer> superSet) {
        for (int i = 1; i <= width; i++) {
            if (isPossible(row, column, i) && !superSet.contains(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    // params
    // row and column specify the location of a cell in the board
    // value is the value that call is to be set to
    // throws
    // IllegalArgumentException
    // if value is not in range [1, width]
    // IndexOutOfBoundsException
    // if either row or column in not in range [0, width)
    protected void setValue(int row, int column, int value) {
        if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }
        if (value < 1 || value > width) {
            throw new IllegalArgumentException();
        }

        // a cell has value x when every value
        // but x has been ruled out
        for (int i = 1; i <= width; i++) {
            if (i != value) {
                setImpossible(row, column, i);
            }
        }
    }

    // params
    // row and column specify the location of a cell in the puzzle board
    // value is the value which said cell is being set to never assume
    // ie value is being removed from the cell's scratch marks
    // modifies
    // removes value from scratch marks of cell at position (row, column)
    // if doing so leaves the cell with only one possible value,
    // all cells in same row, column, and sector are informed they
    // cannot assume said value. this is recursive and will repeat for
    // every cell that is determined as a result of this setting
    // throws
    // IndexOutOfBoundsException if row or column isn't in range [0, width)
    // IllegalArgumentException iff value is not in range [1, width];
    private void setImpossible(int row, int column, int value) {
        if (!isInBounds(row, column)) {
            throw new IndexOutOfBoundsException();
        }
        if (value < 1 || value > width) {
            throw new IllegalArgumentException();
        }

        // get value twice to see if this setting determined cell
        int oldValue = internalGetValue(row, column);
        scratchMarks[row][column][value - 1] = false;
        int newValue = internalGetValue(row, column);

        // if the following is true, the specified cell
        // has just gone from being undetermined to determined
        // so all its row/column/sector-mates must be notified
        // that they need to remove the corresponding scratch mark
        if(oldValue < 1 && newValue >= 1 ) {
            // row-column updates
            for (int i = 0; i < width; i++) {
                if (i != column) {
                    setImpossible(row, i, newValue);
                }
                if (i != row) {
                    setImpossible(i, column, newValue);
                }
            }

            int sectorWidth = (int) Math.sqrt(width);
            int verticalSector = row / sectorWidth;
            int horizontalSector = column / sectorWidth;

            // sector updates
            for (int i = verticalSector * sectorWidth; i < (verticalSector * sectorWidth) + sectorWidth; i++) {
                for (int j = horizontalSector * sectorWidth; j < (horizontalSector * sectorWidth) + sectorWidth; j++) {
                    if (i != row || j != column) {
                        setImpossible(i, j, newValue);
                    }
                }
            }
        }
    }

    // used to store locations of cells in the puzzle
    private static class CellLocation implements Comparable<CellLocation> {
        private int row;
        private int column;

        private CellLocation(int row, int column) {
            this.row = row;
            this.column = column;
        }

        @Override
        // necessary for use in TreeSet
        // compares first on row and then on column
        public int compareTo(CellLocation other) {
            int rowComparison = Integer.compare(this.row, other.row);
            if (rowComparison != 0) {
                return rowComparison;
            } else {
                return  Integer.compare(this.column, other.column);
            }
        }
    }
}