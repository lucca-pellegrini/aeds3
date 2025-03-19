package AEDs3.DataBase;

import java.io.IOException;

public interface Index {
	public void create(int id, long pos) throws IOException;
	public long read(int id) throws IOException;
	public void update(int id, long newPos) throws IOException;
	public void delete(int id) throws IOException;
	public void destruct() throws IOException;
}
