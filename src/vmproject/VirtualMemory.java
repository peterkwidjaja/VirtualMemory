/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vmproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Peter
 */
public class VirtualMemory {
    private final int[] pMem;
    private final int[] bitMap;
    private final int[] MASK;
    private TLB buffer;
    public VirtualMemory(){
        pMem = new int[524288];
        bitMap = new int[32];
        MASK = new int[32];
        MASK[31] = 1;
        for(int i=30; i>=0; i--){
            MASK[i] = MASK[i+1] << 1;
        }
        buffer = new TLB();
    }
    public void initST(String[] in){
        int seg, add;
        markBitmap(0); //frame 0 as occupied
        for(int i=0; i<in.length; i+=2){
            seg = Integer.parseInt(in[i]);
            add = Integer.parseInt(in[i+1]);
            pMem[seg]=add;
            markBitmap(add/512);
            markBitmap((add/512)+1);
        }
    }
    public void initPT(String[] in){
        int page, seg, add;
        for(int i=0; i<in.length;i+=3){
            page = Integer.parseInt(in[i]);
            seg = Integer.parseInt(in[i+1]);
            add = Integer.parseInt(in[i+2]);
            pMem[pMem[seg]+page] = add;
            markBitmap(add/512);
        }
    }
    private int[] translate(int va){
        int[] result = new int[3];
        result[2] = va%(int)(Math.pow(2,9));
        va = va >> 9;
        result[1] = va%(int)(Math.pow(2,10));
        va = va >> 10;
        result[0] = va;
        return result;
    }
    public int read(int va){
        int[] address = translate(va);
        int result = 0;
        for(int i=0;i<2;i++){
            result = pMem[address[i]+result];
            if(result==0)
                return 0;
            else if(result==-1)
                return -1;
        }
        return result+address[2];
    }
    public int readTLB(int va){
        int sp = va >> 9;
        int result = buffer.lookup(sp);
        if(result==-1){
            result = read(va);
            if(result>0){
                buffer.addNew(sp, result-(va%(int)(Math.pow(2,9))));
                System.out.print("m ");
            }
            return result;
        }
        else{
            System.out.print("h ");
            return result + (va%(int)(Math.pow(2,9)));
        }
    }
    //va translation for write command
    public int write(int va){
        int[] address = translate(va);
        int result = 0;
        int pos = 0;
        for(int i=0;i<2;i++){
            pos = address[i] + result;
            result = pMem[pos];
            if(result==-1)
                return -1;
            else if(result==0){
                if(i==0){
                    int newPT = createPT();
                    pMem[address[0]]=newPT;
                    result = newPT;
                }
                else{
                    int newPage = createPage();
                    pMem[pos] = newPage;
                    result = newPage;
                }
            }
        }
        return result+address[2];
    }
    //va translation for write using TLB
    public int writeTLB(int va){
        int sp = va >> 9;
        int result = buffer.lookup(sp);
        if(result==-1){
            result = write(va);
            if(result>0){
                buffer.addNew(sp, result-(va%(int)(Math.pow(2,9))));
                System.out.print("m ");
            }   
            return result;
        }
        else{
            System.out.print("h ");
            return result + (va%(int)(Math.pow(2,9)));
        }
    }
    private int createPT(){
        int emptyFrame = findFrame(1);
        while(emptyFrame!=-1&&!isEmpty(emptyFrame+1)){
            emptyFrame = findFrame(emptyFrame+2);
        }
        if(emptyFrame==-1){
            return -1;
        }
        markBitmap(emptyFrame);
        markBitmap(emptyFrame+1);
        
        return emptyFrame*512;
    }

    private int createPage(){
        int emptyFrame = findFrame(1);
        if(emptyFrame==-1){
            return -1;
        }
        markBitmap(emptyFrame);
        return emptyFrame*512;
    }
    //find empty frame starting from startFrame
    private int findFrame(int startFrame){
        boolean flag=true;
        int startBitmap = startFrame/32;
        int startMask = startFrame%32;
        for(int i=startBitmap; i<bitMap.length; i++){
            for(int j=0; j<32;j++){
                if(flag && i==startBitmap){
                    flag = false;
                    j=startMask;
                }
                if(isEmpty(i,j)){
                    return (i*32)+j;
                }
            }
        }
        return -1;
    }
    
    //return true if the following frame (indicated by bit and mask) is empty
    private boolean isEmpty(int bit, int mask){
        int test = bitMap[bit] & MASK[mask];
        return test==0;
    }
    
    //return true if bitmap shows the frame is empty
    private boolean isEmpty(int frameNo){
        int bit = frameNo/32;
        int mask = frameNo%32;
        int test = bitMap[bit] & MASK[mask];
        return test==0;
    }
    
    //mark bitmap to 1 for occupied frame
    private void markBitmap(int frameNo){
        int bit = frameNo/32;
        int mask = frameNo%32;
        bitMap[bit] = bitMap[bit]|MASK[mask];
    }
    
    //process va translation normally
    public static void processNormal(String[] va, VirtualMemory vm){
        int result;
        for(int i=0; i<va.length; i+=2){
            if(Integer.parseInt(va[i])==0){
                result = vm.read(Integer.parseInt(va[i+1]));
                if(result==-1)
                    System.out.print("pf ");
                else if(result==0)
                    System.out.print("err ");
                else
                    System.out.print(result+" ");
            }
            else{
                result = vm.write(Integer.parseInt(va[i+1]));
                if(result==-1)
                    System.out.print("pf ");
                else
                    System.out.print(result+" ");
            }
        }
    }
    
    //process va translation using TLB
    public static void processTLB(String[] va, VirtualMemory vm){
        int result;
        for(int i=0; i<va.length; i+=2){
            if(Integer.parseInt(va[i])==0){
                result = vm.readTLB(Integer.parseInt(va[i+1]));
                if(result==-1)
                    System.out.print("pf ");
                else if(result==0)
                    System.out.print("err ");
                else
                    System.out.print(result+" ");
            }
            else{
                result = vm.writeTLB(Integer.parseInt(va[i+1]));
                if(result==-1)
                    System.out.print("pf ");
                else
                    System.out.print(result+" ");
            }
        }
    }
    //main method
    public static void main(String[] args) throws FileNotFoundException, IOException{
        VirtualMemory vm = new VirtualMemory();
        BufferedReader brInit = new BufferedReader(new FileReader(new File("input1.txt")));
        String[] input = brInit.readLine().split(" ");
        vm.initST(input);
        input = brInit.readLine().split(" ");
        vm.initPT(input);
        
        BufferedReader br = new BufferedReader(new FileReader(new File("input2.txt")));
        String[] va = br.readLine().split(" ");
        
        if(args.length>0 && args[0].equals("tlb")){
            processTLB(va, vm);
        }
        else{
            processNormal(va, vm);
        }
    }
}
