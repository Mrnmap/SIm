all : sample

sample : sample1.o 
	gcc -o sample sample1.o -lpcsclite -L/usr/local/lib 

sample1.o : sample1.c 
	gcc -o sample1.o -c sample1.c

clean :
	rm -rf *.o sample 

