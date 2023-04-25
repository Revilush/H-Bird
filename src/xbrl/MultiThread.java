package xbrl;

public class MultiThread {

	public class HelloRunnable implements Runnable {

	    public void run() {
	        System.out.println("Hello from a thread!");
	    }

	    public void main(String args[]) {
	        System.out.println("Hello from a thread!");

//	        (new Thread(new HelloRunnable())).start();
	    }
	}
}
