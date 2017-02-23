===============
GVGAI Framework
===============

This is the framework for the General Video Game AI Competition - http://www.gvgai.net/

Google group - https://groups.google.com/forum/#!forum/the-general-video-game-competition

Github repository - https://github.com/EssexUniversityMCTS/gvgai

=================
Hunger Games & AI
=================

Github repository - https://github.com/rdgain/Hunger-Games-VGDL
Gameplay video - 

## To run the game: 
	* Open class TestMultiPlayer.java
	* Line 26, set the 2 players of the game: String controllers = humanController  + " " + RHEA; (this plays a human as player 1 and RHEA as player 2; see Game Modes below)
	* Run class src/TestMultiPlayer.java	

## Game modes (modify line 26 in src/TestMultiPlayer.java):
	* Human vs Human. String controllers = humanController  + " " + humanController;
	* Human vs AI. String controllers = humanController  + " " + RHEA; (any other AI controllers declared in the class may be used)
	* AI vs Human (reversed players). String controllers = RHEA + " " + humanController; (any other AI controllers declared in the class may be used; MUST set playerID parameter - the last one - in line 51 to 1)
	* AI vs AI. String controllers = RHEA  + " " + RHEA; (any other AI controllers declared in the class may be used)
	
## Game description:
	* It's the last 2 survivors of the Hunger Games and only one (or maybe not?) can be the winner!
	* The forest is covered in thick fog, hiding things from the players. But you can reveal the level by moving around, although you only get to see what you reveal and not what the enemy sees!
	* You're constantly starving, watch your health bar.
	* There are killer wolves spawning periodically that chase you around and decrease your health. Use a weapon to kill them.
	* Default weapon: stick. But there are arrows that can be collected in the forest that give you a limited supply of arrows to shoot (ranged weapon).
	* It takes only one hit to kill animals, but several to kill the opponent.
	* In order to increase health:
		- Eat berries. They randomly spawn on the berry bushes in the forest. They give 3 health points.
		- Kill butterflies and eat the food they drop. They give 15 health points.
		- Kill bears and eat the food they drop. They give 30 health points.
		- Kill wolves and eat the food they drop. They give 30 health points.
	* Be careful when killing animals, the food dropped is visible to all players, acting like a signal for the enemy!

## Game techincal implementation:
	* Written in VGDL
	* Framework modifications: 
		- Added the possibility of hiding an object from only one player.
		- Modified UI for human play (+ effect prompts).
	* Fog of war implementation:
		- One 'fog' sprite layered on top of each position in the level.
		- In the beginning, it starts as hidden from both players and transforms all the other objects it overlaps to being hidden from both players.
		- When a player collides with a 'fog' object, they spawn fog removal objects, which transform the 'fog' object from being hidden to both players, to only being hidden to the opponent. The other overlapping objects receive the change from the new partially-hidden fog.
		- If both players visit an area, then the 'fog' turns to an invisible object which signals that both players can see the sprites at that location.
		- Moving objects adjust themselves when they change fog type areas, so they can become visible, then hidden again.
	* Each interaction affecting the health points, as well as revealing the fog, affects the game scoring system as well (as guidance for general agents and human player feedback).
	* Game map generated using simplex noise for interesting area layout and manually adjusted to fit the purpose and include all game objects as needed.

## AI description:
	* Code can be found in package controllers.multiPlayer.RHEA
	* Basic Rolling Horizon Evolutionary Algorithm, using sequences of in-game actions encoded as individuals, which are then evolved through uniform crossover (tournament of 2 used for selection), uniformly at random mutation of 1 gene and promoted through elitism of 1. A population in the final configuration consists of 5 individuals of length 8 (8 sequential in-game actions).
	* The Forward Model provided within the GVGAI framework is used to evaluate an individual: the game state is simulated as if the actions in one individual were executed in order and the final state evaluated using a heuristic. The value of this state becomes the fitness of the individual, which the algorithm attempts to maximise.
	* The algorithm's heuristic depends on the state the agent is currently in. All of the states maximise the game score, minimse the number of NPCs and give a penalty for overlapping the enemy. Additionally, a large bonus is given if they won or a large penalty if they lost. There are 4 states:
		- Default state EXPLORE: Maximises the number of resources collected, the distance moved and the current number of health points; Minimises the distance to the targetPosition. A penalty is applied if the agent did not move at all during the rollout. The targetPosition is chosen as the opposite set of coordinates to where the agent currently is.
		- ATTACK: Minimises the distance to the enemy and maximises the number of resources collected (small weight).
		- DEFEND: Maximises the distance to the enemy, the number of resources collected (small weight) and the number of health points.
		- COLLECT: Maximises the number of resources collected, the number of health points and minimises the distance to the closest resource.
	* State transitions (in this order of priority):
		- (If the opponent is close AND (the number of health points is higher than the opponent OR crazy)) state = ATTACK
		- (If the opponent is close AND (the number of health points is lower than the opponent OR NOT crazy)) state = DEFEND
		- (If an attacking NPC is close) state = ATTACK 
		- (If resource nearby) state = COLLECT
		- Default: EXPLORE
	* Opponent model: random (every time the game is simulated using the Forward Model, it assumes that the opponent will do a random move).