package net.alagris.core.learn;

import net.alagris.core.FuncArg;
import net.alagris.core.IntSeq;
import net.alagris.core.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;

public interface LazyDataset<O> {
    public static final String PYTHON = System.getenv().getOrDefault("PYTHON","python3");
    static LazyDataset<Pair<IntSeq, IntSeq>> loadDatasetFromPython(File file, String separator) {
        return new LazyDataset<Pair<IntSeq, IntSeq>>() {
            Process ps;
            BufferedReader in;
            @Override
            public void close() throws Exception {
                ps.destroy();
                in.close();
            }

            @Override
            public void begin() throws Exception {
                final ProcessBuilder pb = new ProcessBuilder(PYTHON,file.getAbsolutePath());
                ps = pb.start();
                in = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            }

            @Override
            public Pair<IntSeq, IntSeq> next() throws Exception {
                String line = in.readLine();
                if(line==null)return null;
                return FuncArg.parseDatasetLine(line,separator);
            }
        };
    }

    void close() throws Exception;

    void begin() throws Exception;

    O next() throws Exception;

    static <O> LazyDataset<O> from(Iterable<O> iters){
        return new LazyDataset<O>() {
            private Iterator<O> it;

            @Override
            public void close() throws Exception {

            }

            @Override
            public void begin() throws Exception {
                it = iters.iterator();
            }

            @Override
            public O next() throws Exception {
                return it.hasNext()?it.next():null;
            }
        };
    }
    static <O> LazyDataset<O> combine(Iterable<LazyDataset<O>> iters){
        return new LazyDataset<O>() {
            Iterator<LazyDataset<O>> it = null;
            LazyDataset<O> i = null;
            @Override
            public void close() throws Exception {
                if(i!=null)i.close();
            }

            @Override
            public void begin() throws Exception {
                close();
                it = iters.iterator();
                if(it.hasNext())i = it.next();
                if(i!=null)i.begin();
            }

            @Override
            public O next() throws Exception {
                O o;
                while((o=i.next())==null){
                    i.close();
                    if(!it.hasNext())return null;
                    i = it.next();
                    i.begin();
                }
                return o;
            }
        };
    }

    public static LazyDataset<Pair<IntSeq,IntSeq>> loadDatasetFromFile(File f, String separator) {
        return new LazyDataset<Pair<IntSeq, IntSeq>>() {
            Scanner sc ;
            @Override
            public void close() {
                if(sc!=null)sc.close();
            }

            @Override
            public void begin() throws Exception {
                close();
                sc = new Scanner(f);
            }

            @Override
            public Pair<IntSeq, IntSeq> next() {
                if (sc.hasNextLine()) {
                    return FuncArg.parseDatasetLine(sc.nextLine(),separator);
                }
                return null;
            }
        };
    }
}
