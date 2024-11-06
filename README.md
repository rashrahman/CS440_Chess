# Chess AI Bot with Alpha-Beta Pruning
An intelligent chess agent implementation using the Minimax algorithm with Alpha-Beta pruning, custom heuristics, and move ordering optimizations. Built using the Sepia game engine in Java.
Features
### Alpha-Beta Pruning: 
Efficient implementation of the Minimax algorithm with Alpha-Beta pruning for optimal move selection
### Custom Heuristics: 
Advanced board evaluation metrics considering:
- Piece positioning and control
- Board control and territory
- Offensive and defensive strategies
- Material advantage
### Optimized Move Ordering:
Smart move prioritization to maximize Alpha-Beta pruning efficiency
### Performance Tuning:
Depth-adjusted search based on runtime profiling
### Project Structure
Copysrc/pas/chess/
├── agents/
│   ├── MinimaxAgent.java     # Base Minimax implementation
│   └── AlphaBetaAgent.java   # Alpha-Beta pruning implementation
├── heuristics/
│   ├── DefaultHeuristics.java
│   └── CustomHeuristics.java
├── moveorder/
│   ├── DefaultMoveOrderer.java
│   └── CustomMoveOrderer.java
├── instrumentation/
│   └── MinimaxAgent.java     # Performance profiling
└── debug/
    └── MinimaxReflectionAgent.java  # Validation testing
### Setup
Copy required files to your project directory:
chess.jar to lib/chess.jar
Game configurations to data/pas
Source files to src/
Compilation config to chess.srcs
Documentation to doc/
### Compile the project:
#### Mac/Linux
javac -cp "./lib/*:." @chess.srcs
#### Windows
javac -cp .\lib\*;. @chess.srcs
### Run a game:
#### Mac/Linux
java -cp "./lib/*:." edu.cwru.sepia.Main2 data/pas/chess/RandomvsRandom.xml
#### Windows
java -cp .\lib\*;. edu.cwru.sepia.Main2 data/pas/chess/RandomvsRandom.xml
### Technical Details
Game Engine: Built on Sepia, with custom adaptations for chess-specific mechanics
Move Processing: Converts high-level chess moves into sequential Sepia actions
Turn Management: Implements thread-safe agent synchronization for turn-based gameplay
Evaluation Bounds: Heuristic values are bounded between -Double.MAX_VALUE (loss) and +Double.MAX_VALUE (win)
### Testing
Validate the Alpha-Beta implementation against the base Minimax algorithm:
java -cp "./lib/*:." edu.cwru.sepia.Main2 data/pas/chess/debug/ValidateAlphaBetaPruning.xml
### Requirements
Java Development Kit (JDK)
Sepia game engine (included in chess.jar)
### Contributors
Rashfiqur Rahman and Zuizz Saeed

[Your Name]
