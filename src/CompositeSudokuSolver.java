/*
 * CompositeSudokuSolver is a SudokuSolver
 * which, when asked to solve a puzzle,
 * will first soften up the puzzle using
 * fast deductive solving techniques.
 * If this doesn't completely
 * solve the puzzle, a recursive backtracking
 * solution is then used to finish the job.
 * This combines the speed of a deductive solver
 * and the guaranteed solution finding of the backtracking
 * solver to get the best of both worlds.
 * This solver does not rely on the assumption
 * that each puzzle has only one solution.
 * As long as the puzzle has at least one solution,
 * a solution will be found eventually.
 * It also supports puzzles of any size,
 * so long as the puzzle's width is a non-zero
 * perfect square, so 4x4, 9x9, 16x16, etc puzzles
 * are all valid.
 *
 * Pros & Cons of this SudokuSolver Compared to Others:
 * (all clock runtimes provided are based on my own computer and will vary)
 *
 * Pros:
 * - all the speed and consistency of the deductive solver with
 *      the guaranteed solution of the backtracker
 * - median runtime on a 9x9 is roughly 0.01 seconds
 * - 9x9 outliers non-existent (as far I can tell)
 *      no 9x9 I've found has taken longer that 0.06 seconds
 * - contradictory puzzles take no longer to process than others
 * - capable of solving any puzzle eventually
 * - can solve puzzles that have multiple solutions, and fast
 * - solves any 16x16 that the deductive solver can just as quickly
 *
 * Cons:
 * - not all solutions can be explained logically to humans
 * - if a 16x16 puzzle or larger is very difficult to reason through logically,
 *      the deductive portion of this solver will be stumped and then
 *      this solver becomes no better than the Backtracker
 *      and could then take unimaginably long to solve such puzzles
 *
 * All inherited method documentation provided in
 * SudokuSolver.java
 */

import java.io.FileNotFoundException;
import java.io.File;

public class CompositeSudokuSolver extends SudokuSolver {
    //could be DeductiveSudokuSolver or BacktrackingSudokuSolver
    // at any given time
    private SudokuSolver ss;

    // we start with the deductive solver to quickly soften puzzles up
    CompositeSudokuSolver () {
        ss = new DeductiveSudokuSolver();
    }

    @Override
    public boolean loadAndSolveSudoku(File puzzleData)
            throws BadSudokuDataFileException, FileNotFoundException {
        // ensure we start with the deductive solver
        ss = new DeductiveSudokuSolver();
        if (ss.loadAndSolveSudoku(puzzleData)) {  // puzzle solved wholly by logic case
            return true;
        } else if (ss.contradictionWasFound()) {  // puzzle is self-contradictory
            return false;
        } else {  // time to try the backtracker
            // it is necessary that ss be replaced with
            // the new BacktrackingSudokuSolver so that
            // the user can access the board's state
            // after it is solved
            BacktrackingSudokuSolver btss = new BacktrackingSudokuSolver();
            boolean retValue = btss.loadAndSolveSudoku(ss);
            ss = btss;
            return retValue;
        }
    }

    @Override
    public int getValue(int row, int column) {
        return ss.getValue(row, column);
    }

    @Override
    public int getWidth() {
        return ss.getWidth();
    }

    @Override
    public boolean contradictionWasFound() {
        return ss.contradictionWasFound();
    }

    @Override
    protected void setValue(int row, int column, int value) {
        ss.setValue(row, column, value);
    }

    @Override
    protected void initializeBoard(int width) {
        ss.initializeBoard(width);
    }
}