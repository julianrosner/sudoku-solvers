# sudoku-solvers
A Number of Classes which Solve Sudoku Puzzles through Various Means, 
Along with Some Test Puzzles to Run Them On

Any of these solvers is capable of handling puzzles of different dimensions, so long as puzzle width = height
and the width is a perfect square, so 1 x 1, 4 x 4, 9 x 9, 16 x 16, 25 x 25, etc are all acceptable.
Also, any of these solvers should easily solve the kind of sudoku you find in consumer publications.
When it comes to the more hardcore puzzles I found from the sudoku communities online, the deductive solver
could only handle about 70% of those on its own.

The purpose and features for each solver is documented with each solver in great detail,
but to boil each down to its main idea...

BacktrackingSudokuSolver uses recursive backtracking and therefore can theoretically solve any puzzle, no matter how difficult it's solution is to deduce logically (although compared to the others, it is quite slow).

DeductiveSudokuSolver solves puzzles using only human-understandable logic, so it's a good tool to use if you want to know how you could've solved a puzzle. It also tends to be MUCH FASTER than the backtracker.

CompositeSudokuSolver softens up puzzles using DeductiveSudokuSolver, then if any unsolved parts remain, it finishes the puzzle off using the backtracker, making it very fast on the average case, and slightly slower but still un-stumpable in the hardest cases.
