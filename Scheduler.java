import java.util.*;
import java.io.*;

public class Scheduler{
	static boolean verbose = false;
	static Scanner scan;
	static int q = 2;
	static int totalIO = 0;
	static ArrayList info = new ArrayList();
	
	//Created a Process class that can store information for a specific process.
	static class Process{
		private int id;
		private int arrivalTime;
		private int CPUInterval;
		private int CPUTime;
		private int IOInterval;
		private int finishTime;
		private int arrivalInReady;
		private int activeIO;
		private int activeRunning;
		private int preemptRunning;
		private int timeWait;
		private int timeRunning;
		private int timeBlock;
		private boolean running;
		private boolean ready;
		private boolean blocked;
		private boolean done;
		Process(int A, int B, int C, int IO){
			id = 0;
			arrivalTime = A;
			CPUInterval = B;
			CPUTime = C;
			IOInterval = IO;
			finishTime = A;
			arrivalInReady = 0;
			activeIO = 0;
			activeRunning = 0;
			preemptRunning = 0;
			timeWait = 0;
			timeRunning = 0;
			timeBlock = 0;
			running = false;
			ready = false;
			blocked = false;
			done = false;
		}
	}

	public static void main(String [] args){
		ArrayList<Integer> data = new ArrayList<Integer>();
		ArrayList<Process> processes = new ArrayList<Process>();
		File file;

		//If there are 2 arguments, verbose mode is turned on and the file is read in.
		if(args.length > 1){
			verbose = true;
			file = new File(args[1]);
		}
		//Else the file is just read in
		else file = new File(args[0]);
		
		try{
			//If possible, stores data to appropriate variables.
			scan = new Scanner(file);
			int numProcesses = scan.nextInt(); //Number of processes we need to consider
			//Add all data into the data arraylist.
			while(scan.hasNextInt()){
				data.add(scan.nextInt());
			}
			File f = new File("random.txt");

			//Models the scheduling time for a First Come First Serve scheduler.
			init(numProcesses, processes, data, info, verbose);
			scan = new Scanner(f);
			schedule(processes, 1);
			
			//Models the scheduling time for a Round Robin scheduler.
			init(numProcesses, processes, data, info, verbose);
			scan = new Scanner(f);
			schedule(processes, 2);

			//Models the scheduling time for a Last Come First Serve scheduler.
			init(numProcesses, processes, data, info, verbose);
			scan = new Scanner(f);
			schedule(processes, 3);

			//Models the scheduling time for a Highest Penalty Ratio scheduler.
			init(numProcesses, processes, data, info, verbose);
			scan = new Scanner(f);
			schedule(processes, 4);	
		}
		catch(FileNotFoundException e){
			System.err.println ("Unable to read from file");
			System.exit(-1);
		}
	}

	//Initializes array lists based on the input.
	public static void init(int numProcesses, ArrayList<Process> processes, ArrayList<Integer> data, ArrayList info, boolean verbose){
		System.out.println("-------------------------------------------------------------------------------------");
		//Prints the original input
		System.out.print("The original input was: " + numProcesses + " ");
		for(int i = 0; i < data.size(); i++){
			System.out.print(data.get(i) + " ");
		}

		//Creates processes based on the input.
		for(int i = 0; i < numProcesses; i++){
			processes.add(new Process(data.get(4*i), data.get((4*i) + 1), data.get((4*i) + 2), data.get((4*i) + 3)));
		}

		System.out.println();
		//Sorts and prints the sorted input.
		startSort(numProcesses, processes, data);
		printSorted(numProcesses, processes);

		if(verbose) System.out.println("This detailed printout gives the state and remaining burst for each process\n");

		//Fills an info array list for easy of input.
		for(int i = 0; i < 2*processes.size(); i++){
			info.add(null);
		}
	}

	//Sorts the processes based on arrival time
	public static void startSort(int numProcesses, ArrayList<Process> processes, ArrayList<Integer> data){
		int beginning = 0;
		for(int i = 0; i < numProcesses; i++){
			if((i < numProcesses - 1) && (processes.get(beginning).arrivalTime > processes.get(i+1).arrivalTime)){
				processes.add(0, processes.get(i+1));
				processes.remove(i+2);
				beginning++;
			}
			processes.get(i).id = i;
		}
	}

	//Main scheduler method.
	public static void schedule(ArrayList<Process> processes, int type){
		//Initializes variables.
		ArrayList<Process> done = new ArrayList<Process>();
		ArrayList<Process> uses = new ArrayList<Process>();
		uses.add(null);
		int index = 0;
		int num = processes.size();
		int cycle = 0;	
		//Before everything is finshed schedule in the following order. Update blocked processes, update running processes, 
		//update just arriving processes, and finally update ready processes. This ends a cycle.
		while(done.size() != num){
				//doBlocked()
			doBlocked(uses, processes, cycle, type);
				//end doBlocked()
				//doRunning()
			doRunning(done, uses, processes, cycle, type);
				//end doRunning()
				//doArriving()
			doArriving(processes, cycle, type);
				//end doArriving()
				//doReady()
			doReady(uses, processes, cycle, type);
				//end doReady()
			//If verbose, print a detailed output.
			if(verbose) {
				System.out.printf("Before cycle %d: ", cycle);
				for(int i = 0; i < info.size(); i+=2){
					System.out.printf("%s %d ", (String)info.get(i), (Integer)info.get(i+1));
				}
				System.out.println("");

				//If something has finished, update its info and print out its status
				if(done.size() != 0){
					for(int i = 0; i < done.size(); i++){
						int pnum = done.get(i).id;
						info.set(2*pnum, "terminated");
						info.set(((2*pnum) + 1), 0);
					}
				}
			}
			//Until every process has finished, increment the cycle count by 1.
			if(done.size() != num){
				cycle++;
			}
			//System.out.println("end while loop");
		}	
		System.out.println("");
		//Sort the output, print information, and clear arraylists so we can run the next type of scheduler.
		sort(done);
		print(done, cycle, type);
		clear(done, uses, processes, info);
	}

	//This simulates what happens to blocked processes.
	public static void doBlocked(ArrayList<Process> uses, ArrayList<Process> processes, int cycle, int type){
		int size = uses.size();
		//Makes sure the IO count is not double incremented. 
		boolean ioAccounted = false;
		for(int i = 1; i < size; i++){
			if(uses.get(i) != null){
				//If the process is blocked, update info
				uses.get(i).blocked = true;
				if(verbose){
					int pNum = uses.get(i).id;
					info.set(2*pNum, "blocked");
					info.set((2*pNum) + 1, uses.get(i).activeIO);
				}	
				//Decrement IO time, increment total IO time, and increment time "blocked".
				uses.get(i).activeIO--;
				if(!ioAccounted) totalIO++;
				uses.get(i).timeBlock++;
				//Once the process has no more IO wait time, put it in the ready cycle.
				if(uses.get(i).activeIO == 0){
					uses.get(i).arrivalInReady = cycle;
					//If LCFS, place in the front of the ready list, cause the Last entering process is processed first.
					if(type == 3) processes.add(0, uses.get(i));
					//Else it goes to the end
					else processes.add(uses.get(i));
					//Remove the current instance (since it moved to a different place in the list) and decrement i and size, since this process moved.
					uses.remove(i);
					i--;
					size--;
					ioAccounted = true;
				}
				ioAccounted = true;
			}
		}
	}
	
	//This simulates what happens to running processes
	public static void doRunning(ArrayList<Process> done, ArrayList<Process> uses, ArrayList<Process> processes, int cycle, int type){
		//As long as we still have processes to update, update the running processes times.
		if(uses.get(0) != null){
			uses.get(0).running = true;
			if(verbose){
				int pNum = uses.get(0).id;
				info.set(2*pNum, "running");
				if(type == 2){
					if(uses.get(0).activeRunning < q) info.set((2*pNum) + 1, uses.get(0).activeRunning);
					else info.set((2*pNum) + 1, q);
				}
				else{
					info.set((2*pNum) + 1, uses.get(0).activeRunning);
				}
			}

			uses.get(0).activeRunning--;
			uses.get(0).timeRunning++;
			if(type == 2) q--; //If we are using a round robin scheduler, we have to keep the process running for q cycles
			if(uses.get(0).timeRunning == uses.get(0).CPUTime){ //If we reach the CPU time, the process is finished and added to done.
				uses.get(0).finishTime = cycle;
				done.add(uses.get(0));
				uses.set(0, null);
				q = 2;
			}
			else if(uses.get(0).activeRunning == 0){ //Else, turn it to blocked.
				uses.get(0).activeIO = randomOS(uses.get(0).IOInterval);
				uses.add(uses.get(0));
				uses.set(0, null);
				q = 2;
			}
			else if(q == 0){
				//Round Robin Preemption: Once the q cycles is over, put it back in the ready list.
				uses.get(0).arrivalInReady = cycle;
				processes.add(uses.get(0));
				uses.set(0, null);
				q = 2;
			}
		}
	}

	//This simulates what happens to arriving processes
	public static void doArriving(ArrayList<Process> processes, int cycle, int type){
		if(type == 4){//If highest penalty ratio
			int size = processes.size();
			for(int i = 0; i < size; i++){
				if(verbose && !processes.get(i).blocked){
					if(!processes.get(i).ready){
						int pNum = processes.get(i).id;	
						info.set(2*pNum, "unstarted");
						info.set((2*pNum) + 1, 0);
					}
					else{
						int pNum = processes.get(i).id;
						info.set(2*pNum, "ready");
						info.set((2*pNum) + 1, 0);			
					}
				}
				//Set ready to run if possible or not blocked.
				if(cycle >= processes.get(i).arrivalTime){
					processes.get(i).blocked = false;
					if(cycle == processes.get(i).arrivalTime){
						processes.get(i).ready = true;
						processes.get(i).arrivalInReady = cycle;
						processes.add(processes.get(i));
						processes.remove(i);
						i--;
						size--;
					}
				}
			}
		}
		else if(type == 3){//The same thing happens for the other processes with minor differences
			int size = processes.size();
			for(int i = 0; i < size; i++){
				if(verbose && (!processes.get(i).blocked && !processes.get(i).running)){
					if(!processes.get(i).ready){
						int pNum = processes.get(i).id;	
						info.set(2*pNum, "unstarted");
						info.set((2*pNum) + 1, 0);
					}
					else{
						int pNum = processes.get(i).id;
						info.set(2*pNum, "ready");
						info.set((2*pNum) + 1, 0);			
					}
				}
				if(cycle >= processes.get(i).arrivalTime){
					processes.get(i).blocked = false;
					processes.get(i).running = false;
					if(cycle == processes.get(i).arrivalTime){
						processes.get(i).ready = true;
						processes.get(i).arrivalInReady = cycle;
						processes.add(0, processes.get(i));
						processes.remove(i + 1);
					}
				}
			}
			
			boolean found = false;
			int startIndex = 0;
			while(!found){
				if((startIndex < processes.size()) && (!processes.get(startIndex).ready)){
					startIndex++;
				}
				else{
					found = true;	
				}
			}
			//If the process is ready, add it into the uses array.
			if(startIndex < processes.size()){
				if((processes.size() > 0) && (processes.get(startIndex).ready)){
					for(int i = startIndex + 1; i < size; i++){
						if(processes.get(startIndex).arrivalInReady == processes.get(i).arrivalInReady){
							if(processes.get(startIndex).id > processes.get(i).id){
								processes.add(startIndex, processes.get(i));
								processes.set(i, processes.remove(startIndex+1));
							}
						}	
					}
				}
			}
		}
		else{
			int size = processes.size();
			for(int i = 0; i < size; i++){
				if(verbose && (!processes.get(i).blocked && !processes.get(i).running)){
					if(!processes.get(i).ready){
						int pNum = processes.get(i).id;	
						info.set(2*pNum, "unstarted");
						info.set((2*pNum) + 1, 0);
					}
					else{
						int pNum = processes.get(i).id;
						info.set(2*pNum, "ready");
						info.set((2*pNum) + 1, 0);			
					}
				}
				if(cycle >= processes.get(i).arrivalTime){
					processes.get(i).blocked = false;
					processes.get(i).running = false;
					if(cycle == processes.get(i).arrivalTime){
						processes.get(i).ready = true;
						processes.get(i).arrivalInReady = cycle;
						processes.add(processes.get(i));
						processes.remove(i);
						i--;
						size--;
					}
				}
			}
			
			boolean found = false;
			int startIndex = 0;
			while(!found){
				if((startIndex < processes.size()) && (!processes.get(startIndex).ready)){
					startIndex++;
				}
				else{
					found = true;	
				}
			}
			if(startIndex < processes.size()){
				if((processes.size() > 0) && (processes.get(startIndex).ready)){
					for(int i = startIndex + 1; i < size; i++){
						if(processes.get(startIndex).arrivalInReady == processes.get(i).arrivalInReady){
							if(processes.get(startIndex).id > processes.get(i).id){
								processes.add(startIndex, processes.get(i));
								processes.set(i, processes.remove(startIndex+1));
							}
						}	
					}
				}
			}	
		}
	}

	public static void doReady(ArrayList<Process> uses, ArrayList<Process> processes, int cycle, int type){
		int c = 0;
		
		if(uses.get(0) == null){
			if((type == 1) || (type == 3)){ // If FCFS or LCFS, take the first process and move it to the uses array.
				while((c < processes.size()) && (!processes.get(c).ready)){
					c++;
				}
				if((c < processes.size()) && (processes.get(c).ready)){
					processes.get(c).activeRunning = randomOS(processes.get(c).CPUInterval);
					uses.set(0, processes.get(c));
					processes.remove(c);
				}
			}
			else if(type == 2){
				while((c < processes.size()) && (!processes.get(c).ready)){
					c++;
				}
				if((c < processes.size()) && (processes.get(c).ready)){ //If the q isn't finished, don't change the active running time.
					if(processes.get(c).activeRunning == 0) processes.get(c).activeRunning = randomOS(processes.get(c).CPUInterval);
					uses.set(0, processes.get(c));
					processes.remove(c);
				}
			}
			else{ //If highest penalty ratio, things change a lot.
				int tempIndex = 0;
				double maxRatio = 0.0;
				//Find which process has the highest penalty ratio, which is the time it's spent in the system divided by the time spent running
				while(c < processes.size()){
					if(processes.get(c).ready){
						double timeInSys = (double)cycle - (double)processes.get(c).arrivalTime;
						if(processes.get(c).timeRunning == 0){
							if(timeInSys > maxRatio){
								maxRatio = timeInSys;
								tempIndex = c;
							}
						}
						else if((timeInSys/(double)processes.get(c).timeRunning) >= maxRatio){
							if((timeInSys/(double)processes.get(c).timeRunning) == maxRatio){
								if(processes.get(c).id < processes.get(tempIndex).id){
									maxRatio = (timeInSys/(double)processes.get(c).timeRunning);
						     			tempIndex = c;
								}
							}
							else{
								maxRatio = (timeInSys/(double)processes.get(c).timeRunning);
								tempIndex = c;
							}
						}
					}
					c++;
				}
				if(cycle != 0){ //Only use this if not at the first cycle.
					if((tempIndex < processes.size()) && (processes.get(tempIndex).ready)){
						processes.get(tempIndex).activeRunning = randomOS(processes.get(tempIndex).CPUInterval);
						uses.set(0, processes.get(tempIndex));
						processes.remove(tempIndex);
						processes.trimToSize();
					}
				}
				else{ //Use the first process.
					c = 0;
					while((c < processes.size()) && (!processes.get(c).ready)){
						c++;
					}
					if((c < processes.size()) && (processes.get(c).ready)){
						processes.get(c).activeRunning = randomOS(processes.get(c).CPUInterval);
						uses.set(0, processes.get(c));
						processes.remove(c);
					}
				}
			}
		}
		c = 0;
		while(c < processes.size()){
			if(processes.get(c).ready){ //Once a process has been added, all other ready processes are "waiting"
				processes.get(c).timeWait++;
			}
			c++;
		}
	}

	//Sorts the finished array based on process ID.
	public static void sort(ArrayList<Process> done){
		int size = done.size();
		ArrayList<Process> temp = new ArrayList<Process>();
		for(int i = 0; i < size; i++){
			temp.add(null);
		}
		for(int i = 0; i < size; i++){
			temp.set(done.get(i).id, done.get(i));
		}
		for(int i = 0; i < size; i++){
			done.set(i, temp.get(i));
		}
	}

	//Prints the information gained after running all scheduler methods.
	public static void print(ArrayList<Process> done, int cycle, int type){
		if(type == 1) System.out.println("The scheduling algorithm used was First Come First Served");
		else if(type == 2) System.out.println("The scheduling algorithm used was Round Robin (with quantum 2)");
		else if(type == 3) System.out.println("The scheduling algorithm used was Last Come First Served");
		else System.out.println("The scheduling algorithm used was Highest Priority Ratio Next");
		for(int i = 0; i < done.size(); i++){
			System.out.println("");
			System.out.printf("Process %d:\n(A,B,C,IO) = (%d,%d,%d,%d)\n", i, done.get(i).arrivalTime, done.get(i).CPUInterval, done.get(i).CPUTime, done.get(i).IOInterval);
			System.out.printf("Finishing time: %d\nTurnaround time: %d\nI/O time: %d\nWaiting time: %d\n", done.get(i).finishTime, (done.get(i).finishTime-done.get(i).arrivalTime), done.get(i).timeBlock, done.get(i).timeWait);
		}
		double cpu = 0.0;
		double io;
		double throughput = (double)(done.size() * 100) / (double)cycle;
		double avgTurn = 0.0;
		double avgWait = 0.0;
		for(int i = 0; i < done.size(); i++){
			cpu += done.get(i).timeRunning;
			//io += done.get(i).timeBlock;
			avgTurn += done.get(i).finishTime - done.get(i).arrivalTime;
			avgWait += done.get(i).timeWait;
		}
		cpu = cpu/(double)cycle;
		io = (double)totalIO/(double)cycle;
		avgTurn = avgTurn/(double)done.size();
		avgWait = avgWait/(double)done.size();
		System.out.println("");
		System.out.printf("Summary Data:\nFinishing time: %d\nCPU Utilization: %f\nI/O Utilization: %f\n", cycle, cpu, io);
		System.out.printf("Throughput: %f processes per hundred cycles\nAverage turnaround time: %f\nAverage waiting time: %f\n", throughput, avgTurn, avgWait);
	}

	//Prints the input in sorted order.
	public static void printSorted(int numProcesses, ArrayList<Process> p){
		System.out.print("The (sorted) input is:  " + numProcesses + " ");
		for(int i = 0; i < numProcesses; i++){
			System.out.printf("%d %d %d %d ", p.get(i).arrivalTime, p.get(i).CPUInterval, p.get(i).CPUTime, p.get(i).IOInterval); 
		}
		System.out.println();
	}

	//Clears all arraylists and necessary variables.
	public static void clear(ArrayList<Process> done, ArrayList<Process> uses, ArrayList<Process> processes, ArrayList info){
		done.clear();
		uses.clear();
		processes.clear();
		info.clear();
		totalIO = 0;
	}

	//Chooses a "random" number. Used input from a file to make debugging easier. This could easily be replaced with a random number.
	public static int randomOS(int u){
		int x = scan.nextInt();
		return(1 + (x % u));	
	}
}
