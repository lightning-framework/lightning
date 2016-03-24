package lightning.io;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapping reader implements a Reader which efficiently yields a prologue, the contents of the
 * wrapped reader, and then an epilogue.
 */
public class WrappingReader extends Reader {
    private final Reader originalReader;
    private final char[] prologue;
    private final char[] epilogue;
    private int pos = 0;
    private int firstEpilogueChar = -1;
    private boolean closed = false;

    public WrappingReader(Reader originalReader, char[] prologue, char[] epilogue, Object lock) {
        super(lock);
        this.originalReader = originalReader;
        this.prologue = prologue;
        this.epilogue = epilogue;
    }

    public WrappingReader(Reader originalReader, char[] prologue, char[] epilogue) {
        this.originalReader = originalReader;
        this.prologue = prologue;
        this.epilogue = epilogue;
    }

    public WrappingReader(Reader originalReader, String prologue, String epilogue, Object lock) {
        super(lock);
        this.originalReader = originalReader;
        this.prologue = prologue.toCharArray();
        this.epilogue = epilogue.toCharArray();
    }

    public WrappingReader(Reader originalReader, String prologue, String epilogue) {
        this.originalReader = originalReader;
        this.prologue = prologue.toCharArray();
        this.epilogue = epilogue.toCharArray();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Reader has been closed already");
        }
        int oldPos = pos;
        Logger.getLogger(getClass().getName()).log(Level.FINE, String.format("Reading %d characters from position %d", len, pos));
        if (pos < this.prologue.length) {
            final int toCopy = Math.min(this.prologue.length - pos, len);
            Logger.getLogger(getClass().getName()).log(Level.FINE, String.format("Copying %d characters from prologue", toCopy));
            System.arraycopy(this.prologue, pos, cbuf, off, toCopy);
            pos += toCopy;
            if (toCopy == len) {
                Logger.getLogger(getClass().getName()).log(Level.FINE, "Copied from prologue only");
                return len;
            }
        }
        if (firstEpilogueChar == -1) {
            final int copiedSoFar = pos - oldPos;
            final int read = originalReader.read(cbuf, off + copiedSoFar, len - copiedSoFar);
            Logger.getLogger(getClass().getName()).log(Level.FINE, String.format("Got %d characters from delegate", read));
            if (read != -1) {
                pos += read;
                if (pos - oldPos == len) {
                    Logger.getLogger(getClass().getName()).log(Level.FINE, "We do not reach epilogue");
                    return len;
                }
            }
            firstEpilogueChar = pos;
        }
        final int copiedSoFar = pos - oldPos;
        final int epiloguePos = pos - firstEpilogueChar;
        final int toCopy = Math.min(this.epilogue.length - epiloguePos, len - copiedSoFar);
        if((toCopy <= 0) && (copiedSoFar == 0)) {
            return -1;            
        }
        Logger.getLogger(getClass().getName()).log(Level.FINE, String.format("Copying %d characters from epilogue", toCopy));
        System.arraycopy(this.epilogue, epiloguePos, cbuf, off + copiedSoFar, toCopy);
        pos += toCopy;
        Logger.getLogger(getClass().getName()).log(Level.FINE, String.format("Copied %d characters, now at position %d", pos-oldPos, pos));
        return pos - oldPos;
    }

    @Override
    public void close() throws IOException {
        originalReader.close();
        closed = true;
    }
}