/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package vmproject;

import java.util.HashMap;

/**
 *
 * @author Peter
 */
public class TLB {
    private class Block{
        int lru;
        int frameNo;
        int sp;
        public Block(int sp, int lru, int frameNo){
            this.lru = lru;
            this.frameNo = frameNo;
            this.sp = sp;
        }
    }
    HashMap<Integer, Block> cache;
    Block[] priority;
    int entry;
    public TLB(){
        entry = 0;
        cache = new HashMap<>();
        priority = new Block[4];
    }
    public int lookup(int sp){
        if(cache.containsKey(sp)){
            Block temp = cache.get(sp);
            int oldLru = temp.lru;
            temp.lru = 3;
            for(int i=oldLru; i<3; i++){
                priority[i] = priority[i+1];
                priority[i].lru--;
            }
            priority[3] = temp;
            return temp.frameNo;
        }
        //if no match, return -1 to main 
        else{
            return -1;
        }
    }
    public void addNew(int sp, int frameNo){
        Block newBlock = new Block(sp, 3, frameNo);
        if(entry<4){
            entry++;
        }
        else{
            cache.remove(priority[0].sp);
        }
        for(int i=0; i<3; i++){
            priority[i] = priority[i+1];
            if(priority[i]!=null)
                priority[i].lru--;
        }
        priority[3] = newBlock;
        cache.put(sp, newBlock);
    }
}
