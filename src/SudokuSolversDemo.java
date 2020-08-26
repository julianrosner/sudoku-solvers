/*
 * Simple comparison of all SudokuSolver
 * implementations on a bunch of puzzles of various sizes.
 * This comparison is somewhat hampered, unfortunately
 * by the fact that BacktrackingSudokuSolver (or BSS) is so slow
 * at solving 16x16s and puzzle with no solution that
 * I've had to make BSS skip attempting them at all, else
 * this demo would take hours to complete.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

public class SudokuSolversDemo {

    private static final File PUZZLE_ROOT_DIR = new File("SudokuPuzzles/");
    private static final boolean DISPLAY_SOLVED_PUZZLES = false;

    public static void main(String[] args)
            throws SudokuSolver.BadSudokuDataFileException, FileNotFoundException {
        // get list of all files recursively nested in puzzleDir
        List<File> files = getFileList();

        System.out.println("Running Tests...");
        System.out.println("This Will Take About 20 Seconds...");
        System.out.println();

        // evaluate Backtracking solution
        // we don't include 16 x 16 puzzles for this solver
        // because it is too slow at them
        BacktrackingSudokuSolver bss = new BacktrackingSudokuSolver();
        evaluateSolver(files, bss, "Backtracking", false);

        // evaluate Deductive solution
        DeductiveSudokuSolver dss = new DeductiveSudokuSolver();
        evaluateSolver(files, dss, "Deductive", true);

        // evaluate Composite solution
        CompositeSudokuSolver css = new CompositeSudokuSolver();
        evaluateSolver(files, css, "Composite", true);
    }

    // runs provided SudokuSolver on provided File List
    // then prints out a message describing how many
    // puzzles could and couldn't be solved and
    // how long the processes took.
    // if DISPLAY_SOLVED_PUZZLES, prints all puzzle solutions
    // (or partial solutions) with file names
    //
    // params
    // files is the list of sdku files we're testing on
    // ss is the SudokuSolver being tested
    // variant is a string which describes what kind
    // of solver was provided
    // includeTough indicates whether we should test this
    // solver on time-consuming 16x16's and puzzles with no solution
    private static void evaluateSolver
            (List<File> files, SudokuSolver ss, String variant, boolean includeTough)
            throws SudokuSolver.BadSudokuDataFileException, FileNotFoundException {
        int numberSolvedPuzzles = 0;
        double allStartTime = System.nanoTime();
        StringBuilder sb = new StringBuilder();

        // attempt each puzzle and make note of info about attempt
        for (File f : files) {
            // this is pretty inelegant but I felt it was important
            // to demonstrate that Deductive and Composite can
            // deal with 16x16s and no-solution puzzles
            // really well without making this demo
            // take hours by having Backtracking solve them all too
            if (!includeTough &&
                    (f.getName().startsWith("16") || f.getName().contains("NO_SOLUTION"))) {
                sb.append("SKIPPING: ").append(f.getName()).append("\n");

                // add empty line to match spacing of other list entries
                if (DISPLAY_SOLVED_PUZZLES) {
                    sb.append("\n");
                }
                continue;
            }

            // solve this puzzle and measure how long it took
            double puzzleStartTime = System.nanoTime();
            boolean solved = ss.loadAndSolveSudoku(f);
            if (solved) {
                numberSolvedPuzzles++;
            }
            double puzzleEndTime = System.nanoTime();
            double duration = (puzzleEndTime - puzzleStartTime) / 1000000000.0;

            // add puzzle's info to StringBuilder
            sb.append(f.getName()).append(", ");
            sb.append(duration).append( " seconds, ");
            if (solved) {
                sb.append("SOLVED\n");
            } else {
                sb.append("NOT SOLVED\n");
            }

            if (DISPLAY_SOLVED_PUZZLES) {
               sb.append("Solution:\n").append(ss).append("\n\n");
            }
        }

        // print summary
        double allEndTime = System.nanoTime();
        double duration = (allEndTime - allStartTime) / 1000000000.0;
        System.out.println(variant + "SudokuSolver Solved " + numberSolvedPuzzles
                + "/" + files.size() + " Puzzles in " + duration + " seconds");

        System.out.println(variant + " Puzzle Results:");
        System.out.println(sb);
    }

    // takes a directory and recursively adds all 
    // non-directory files it contains to a list
    // which is then returned
    private static List<File> getFileList() {
        List<File> files = new LinkedList<File>();
        getFileListHelper(PUZZLE_ROOT_DIR, files);
        return files;
    }

    // recursive helper to getFileList
    private static void getFileListHelper(File puzzleSubDir, List<File> files) {
        for (File puzzleFile : puzzleSubDir.listFiles()) {
            if (puzzleFile.isDirectory()) {
                getFileListHelper(puzzleFile, files);
            } else {
                files.add(puzzleFile);
            }
        }
    }
}