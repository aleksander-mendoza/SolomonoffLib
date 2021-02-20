package net.alagris.core.learn;

import net.alagris.core.Pair;

public class MatrixIndexing {
    public static int rectMatrixSize(int rows,int cols) {
        return rows*cols;
    }

    public static int rectMatrixIndex(int row,int col,int totalCols) {
        assert col<totalCols;
        return row*totalCols+col;
    }
    public static int rectMatrixRow(int idx, int totalCols) {
        return idx / totalCols;
    }
    public static int rectMatrixCol(int idx, int totalCols) {
        return idx % totalCols;
    }

    public static int lowerTriangleSize(int sideLength) {
        return (sideLength - 1) * sideLength / 2;//=0+1+2+...+(sideLength-1)
    }
    public static int lowerWithDiagonalTriangleSize(int sideLength) {
        return (sideLength + 1) * sideLength / 2;//=1+2+...+sideLength
    }
    public static int lowerTriangleIndex(int r, int c) {
        assert r != c;
        assert r >= 0;
        assert c >= 0;
        if (r < c) {
            int tmp = r;
            r = c;
            c = tmp;
        }
        assert r > 0;
        assert c >= 0;
        final int row = r - 1;
        final int col = c;
        return lowerWithDiagonalTriangleIndex(row, col);
    }

    public static int lowerWithDiagonalTriangleIndex(int row, int col) {

        if (col > row) {
            return (1 + col) * col / 2 + row;
        }
        return (1 + row) * row / 2 + col;
    }

    public static Pair.IntPair lowerTriagleCell(int idx) {
        final Pair.IntPair p = lowerWithDiagonalTriagleCell(idx);
        final int r = p.l + 1;
        final int c = p.r;
        assert c != r;
        assert c < r;
        assert r > 0;
        assert c >= 0;
        return Pair.of(r, c);
    }

    public static Pair.IntPair lowerWithDiagonalTriagleCell(int idx) {
        // Lower triangular matrix
        // x0 0  0  0
        // x1 x2 0  0
        // x3 x4 x5 0
        // x6 x7 x8 x9
        //
        // idx=0, row(0)=0, col(0)=0
        // idx=0, row(1)=1, col(1)=0
        // idx=0, row(2)=1, col(2)=1
        // idx=0, row(3)=2, col(3)=0
        //
        // Lower triangular matrix without diagonal
        //
        // r = row+1
        // c = col
        // 0  0  0  0  0
        // x0 0  0  0  0
        // x1 x2 0  0  0
        // x3 x4 x5 0  0
        // x6 x7 x8 x9 0
        //
        // idx=0, r(0)=1, c(0)=0
        // idx=1, r(1)=2, c(1)=0
        // idx=2, r(2)=2, c(2)=1
        // idx=3, r(3)=3, c(3)=0
        //
        // (1+row)*row/2+col=idx
        // total number of cells contained in all rows up to
        // the given row (exclusive) is
        // (1+row)*row/2=numberOfCells
        // solution to quadratic formula is
        // row = (sqrt(8*numberOfCells+1)-1)/2
        //
        // notice that if col==0 then
        // (1+row)*row/2=idx=numberOfCells
        // is the total number of cells contained in all rows
        // up to that given row (exclusive)
        //
        // Hence
        // row = (sqrt(8*(1+row)*row/2+1)-1)/2
        // and so the expression above must be a natural number.
        //
        // if idx is between (1+row)*row/2 and (1+(row+1))*(row+1)/2
        // then the sqrt will return some fraction.
        // Hence we could use integer-based square root to get row
        // row = (sqrt(8*idx+1)-1)/2
        // and then column is just
        // col = idx - (1+row)*row/2
        //
        // TODO: the int_sqrt function can be faster than Math.sqrt on floats

        final int row = ((int) (Math.sqrt(8 * idx + 1) - 1)) / 2;
        final int col = idx - (1 + row) * row / 2;
        assert col <= row;
        assert row >= 0;
        assert col >= 0;
        return Pair.of(row, col);
    }
}
