### Summary
The approach I followed for solving the problem of finding all of the prime numbers from
0 to a limit (in this case 10^8) was implementing the 
[Sieve of Eratosthenes](https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes) algorithm using 8 parallell 
threads. This algorithm works on the basis that you go
sequentially from 2 to the square root of the limit you want to find prime numbers within and maintain a 
boolean array with the value at each index indicating whether or not that value is prime. The boolean array 
begins as all true, and for each value you come across thats true you then mark every multiple of that value as 
false in the boolean array. In order to complete this using multithreading, we spin up 8 threads and keep a 
shared counter that implements the java AtomicInteger class so that the value of the count is locked and 
unlocked when each thread updates it to avoid race cases. This enable each thread to work efficiently and do a 
consistent amount of work so they are all fully utilized. As evidenced by the runtimes in the experimental 
evaluation section, the implementation of 8 threads saw a 3x performance improvement over using just one 
showing a clear performance imporvement.

### Experimental Evaluation
Threads | 1 | 2 | 4 | 8
--- | --- | --- | --- |---
Runtime | 670ms | 356ms | 256ms | 217ms