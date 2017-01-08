
/*
 * The following is an original implementation of
 * simulating a Simple Genetic Algorithm as described in
 * [Michalewicz, 1996].
 *
 * Written by: Liam Pimlott
 *
 */
import java.util.*;

public class SGA {

	public static void main(String[] args) {

		char[] goal = { 'h','e','l','l','o',' ','w','o','r','l','d'};

		long startTime = System.currentTimeMillis();
		Generation initGen = new Generation(50, 0.25f, 0.01f, goal);
		initGen.printGen();
		Generation nextGen = new Generation(initGen);
		int Gen = 2;
		while(!nextGen.solutionFound()){
			nextGen = new Generation(nextGen);
			Gen++;
		}
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;

		System.out.println("Chromosome "+nextGen.getFittestIndex()+" of Generation "+Gen+" is optimal\n");
		System.out.println("Total Runtime: "+totalTime+" milliseconds\n");

		nextGen.printGen();
	}
}// End class SGA

class Generation{

	private final int NUM_CHARS = 26; // 0-25 is alphabet we will count 26 as space.
	private final int CHAR_OFFSET = 97; //offset of lowercase in ascii

	private int N;//pop size
	private int lngth;// chromosome length
	private char[][] pop;// current population
	private char[] goal;// goal chromosome character array
 	private int[] fitness;// fitness of current population
 	private float pC;// probability of crossover
 	private float pM;// probability of mutation
 	private boolean solutionFound;

 	// Constructs a new child Generation from a parent Generation
 	public Generation(Generation parents){
 		this.N = parents.getN();
 		this.pC = parents.getPC();
 		this.pM = parents.getPM();
 		this.goal = parents.getGoal();
 		this.lngth = parents.lngth;
 		this.pop = new char[N][lngth];
 		this.solutionFound = false;
 		//Apply the selection process.
 		selection(parents);
 		//Now we select some chromosomes and aplly crossover.
 		crossover();
 		//Now we apply the mutation operator.
 		mutation();
 		//Calculate fitness of each new chromosome.
 		fitness = evaluate();
 	}

 	// Initial Constructor
	public Generation(int N, float pC, float pM, char[] goal){
		this.N = N;
		this.pC = pC;
		this.pM = pM;
		this.goal = goal;
		this.lngth = goal.length;
		this.solutionFound = false;
		pop = new char[N][lngth];
		//Generating random chromosomes
		for(int i = 0; i<N; i++){
			for(int j = 0; j<lngth; j++){
				int random = (int)(Math.random()*(NUM_CHARS+1));
				if(random == NUM_CHARS){
					pop[i][j] = (char)(32);
				} else{
					pop[i][j] = (char)(CHAR_OFFSET + random);
				}
			}// chromosome fill loop end.
		}// population fill loop end.
		//Calculate fitness of each chromosome.
		fitness = evaluate();
	}// initial constructor end.

	// Applies the selection formula to select which parents will reproduce.
	private void selection(Generation parents){
		//Cumulative probabilities of parent generations fitness.
 		float[] cumuProbs = parents.cumuProbs();
 		//Selection Mechanism, selects
 		for(int i = 0; i<parents.getPopulation().length; i++){
 			float random = (float)Math.random();
 			if(random <= cumuProbs[0]){
 				pop[i] = parents.getPopulation()[0];
 			} else{
 				for(int j = 1; j<cumuProbs.length; j++){
 	 	 			if(cumuProbs[j-1] < random && random <= cumuProbs[j]){
 	 	 				pop[i] = deepCopy(parents.getPopulation()[j]);
 	 	 			}
 	 	 		}
 			}
 		}// End selection loop.
	}// End selection method.

	// Applies the crossover operator to a randomly selected even group of chromosomes
	private void crossover(){
		ArrayList<Integer> indices = new ArrayList<Integer>();
		//Choosing which chromosomes will be crossed over.
		for(int i = 0; i<pop.length; i++){
			float random = (float)Math.random();
			//System.out.println(random);
			if(random < pC){
				indices.add(i);
			}
		}
		//Removing a randomly selected chromosome if there is an uneven number
		if(indices.size()%2 != 0){
			int toRem = (int)(Math.random()*(indices.size()));
			indices.remove(toRem);
		}
		//Crossing over
		for(int i = 0; i<indices.size(); i+=2){
			//randomely selecting a cross point
			int crossPoint = (int)(Math.random()*(lngth+1));
			char[] new1 = new char[lngth];
			char[] new2 = new char[lngth];
			//Swapping first chromosome.
			for(int j = 0; j<crossPoint; j++){
				char temp = pop[indices.get(i)][j];
				//new1[j] = '%';
				new1[j] = temp;
			}
			for(int j = crossPoint; j<lngth; j++){
				char temp = pop[indices.get(i+1)][j];
				new1[j] = temp;
			}
			//Swapping second chromosome.
			for(int j = 0; j<crossPoint; j++){
				char temp = pop[indices.get(i+1)][j];
				new2[j] = temp;
			}
			for(int j = crossPoint; j<lngth; j++){
				char temp = pop[indices.get(i)][j];
				new2[j] = temp;
			}
			pop[indices.get(i)] = new1;
			pop[indices.get(i+1)] = new2;
		}// Crossover loop end.
	}// End method crossover

	// Applies the mutation operator to all characters, based on pM
	private void mutation(){
		for(int i = 0; i<N; i++){
			for(int j = 0; j<lngth;	j++){
				float random = (float)Math.random();
				if(random < pM){
					int randomChar = (int)(Math.random()*(NUM_CHARS+1));
					if(randomChar == NUM_CHARS){
						pop[i][j] = (char)(32);
					} else{
						pop[i][j] = (char)(CHAR_OFFSET + randomChar);
					}
				}
			}
		}
	}

	// Returns list with the cumulative probabilities of each chromosome.
	public float[] cumuProbs(){

		float totalFit = (float)totalFitness();
		float[] cumuProbs = new float[fitness.length];

		for(int i = 0; i<cumuProbs.length; i++){
			cumuProbs[i] = fitness[i]/totalFit;
			for(int j = 0; j<i; j++){
				cumuProbs[i] += fitness[j]/totalFit;
			}
			//System.out.println("CUM PROB "+i+": "+cumuProbs[i]);
		}
		return cumuProbs;
	}

	// Evaluates fitness of each chromosome and return an array containing each ones fitness.
	public int[] evaluate(){
		int[] fitness = new int[pop.length];

		for(int i = 0; i<pop.length; i++){
			for(int j = 0; j<pop[i].length; j++){
				if(pop[i][j] == goal[j]){
					fitness[i] += 1;
				}
			}
			if(fitness[i] == lngth){
				solutionFound = true;
			}
		}
		return fitness;
	}

	// Returns total fitness of the Generation
	public int totalFitness(){
		int result = 0;
		for(int i = 0; i<fitness.length; i++){
			result += fitness[i];
		}
		return result;
	}

	// Getters
	public char[][] getPopulation(){ return pop;}
	public int getN(){return N;}
	public float getPC(){return pC;}
	public float getPM(){return pM;}
	public int getLngth(){return lngth;}
	public char[] getGoal(){return goal;}
	public boolean solutionFound(){return solutionFound;}
	public int getFittestIndex(){
		int fittest = fitness[0];
		int index = 0;
		for(int i = 1; i<fitness.length; i++){
			if(fitness[i] > fittest){
				fittest = fitness[i];
				index = i;
			}
		}
		return index;
	}

	// Print
	public void printGen(){
		for(int i = 0; i<pop.length; i++){
			System.out.print("c"+i+" : ");
			for(int j = 0; j< pop[i].length; j++){
				System.out.print(pop[i][j]);
			}
			System.out.print("\n");
		}
	}
	// Chromosome cloner.
	public char[] deepCopy(char[] orig){
		char[] copy = new char[orig.length];
		for(int i = 0; i<orig.length; i++){
			char temp = orig[i];
			copy[i] = temp;
		}
		return copy;
	}

}// End Class Generation.
