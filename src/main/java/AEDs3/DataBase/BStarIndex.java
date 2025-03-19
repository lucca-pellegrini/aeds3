package AEDs3.DataBase;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BStarIndex implements Index {
	TrackDB db;
	RandomAccessFile file;
	String filePath;
	int order;
	int pageSize;
	long root;

	public BStarIndex(TrackDB db) throws FileNotFoundException, IOException {
		this.db = db;
		this.filePath = db.getFilePath() + ".BS.idx";
		this.file = new RandomAccessFile(filePath, "rw");

		if (this.file.length() <= 0)
			throw new IllegalArgumentException("Arquivo vazio. É necessário especificar a ordem.");

		file.seek(0);
		this.root = file.readLong();
		this.order = file.readInt();
		this.pageSize = file.readInt();
	}

	public BStarIndex(TrackDB db, int order) throws FileNotFoundException, IOException {
		this.db = db;
		this.order = order;
		this.root = -1;
		this.filePath = db.getFilePath() + ".BS.idx";
		this.file = new RandomAccessFile(filePath, "rw");

		if (this.file.length() > 0)
			throw new IllegalArgumentException("Arquivo já existe. É impossível setar a ordem.");

		// Determine o tamanho dos setores do sistema de arquivos.
		int blockSize;
		try {
			blockSize = (int) Files.getFileStore(Paths.get(filePath)).getBlockSize();
		} catch (UnsupportedOperationException e) {
			blockSize = 4096;
		}

		this.pageSize = (Integer.SIZE + order * (Long.SIZE) + (order - 1) * (Integer.SIZE + Long.SIZE)) / 8;
		this.pageSize += this.pageSize % blockSize;

		file.seek(0);
		file.writeLong(this.root);
		file.writeInt(this.order);
		file.writeInt(pageSize);
		file.write(new byte[blockSize - (Long.SIZE + 2 * Integer.SIZE) / 8]); // Padding.
	}

	public long read(int id) throws IOException {
		byte[] buffer = new byte[pageSize];
		file.seek(root);

		while (true) {
			file.readFully(buffer);
			Page i = new Page(new ByteArrayInputStream(buffer));
			long ret = i.find(id);

			if (ret == 0)
				return -1;
			else if (ret >= 0)
				return ret;
			else
				file.seek(-ret);
		}
	}

	public void destruct() throws IOException {
		file.close();
		Files.delete(Paths.get(filePath));
		db = null;
		file = null;
	}
}

class Page {
	ObjectInputStream objStream;
	int numItems;

	public Page(ByteArrayInputStream stream) throws IOException {
		objStream = new ObjectInputStream(stream);
		numItems = objStream.readInt();
	}

	public long find(int id) throws IOException {
		for (int i = 0; i < numItems; ++i) {
			long leftChild = objStream.readLong();
			int currentId = objStream.readInt();
			long pos = objStream.readLong();

			if (currentId == id) {
				return pos;
			} else if (currentId > id) {
				if (leftChild >= 0)
					// Usamos retorno negativo para indicar que pode estar no filho.
					return -leftChild;
				else
					return 0;
			}
		}

		long lastChild = objStream.readLong();
		return (lastChild >= 0) ? -lastChild : 0;
	}
}
