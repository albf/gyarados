#include <algorithm>
#include <vector>
#include <map>

#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/IntrinsicInst.h"
#include "llvm/Pass.h"
#include "llvm/Support/CFG.h" // for Moco
//#include "llvm/IR/CFG.h" // for IC
#include "llvm/Support/InstIterator.h"
//#include "llvm/IR/InstIterator.h" // for IC
#include "llvm/Support/raw_ostream.h"

using namespace llvm;
using namespace std;

#define debug true

namespace {
    // Makes Union of vectors.
    vector<Instruction *> Difference(vector<Instruction *>& vec_1, vector<Instruction *>& vec_2) {
        vector<Instruction *> u;

        for(vector<Instruction*>::iterator elem = vec_1.begin(); elem != vec_1.end(); elem++) {
            if(find(vec_2.begin(), vec_2.end(), *elem) == vec_2.end()) {
                u.push_back(*elem);
            }
        }
       return u; 
    }

    // Makes difference of vectors.
    vector<Instruction *> Union(vector<Instruction *>& vec_1, vector<Instruction *>& vec_2) {
        vector<Instruction *> u;

        for(vector<Instruction*>::iterator elem = vec_1.begin(); elem != vec_1.end(); elem++) {
            u.push_back(*elem);
        }
        for(vector<Instruction*>::iterator elem = vec_2.begin(); elem != vec_2.end(); elem++) {
            if(find(vec_1.begin(), vec_1.end(), *elem) == vec_1.end()) {
                u.push_back(*elem);
            }
        }
       return u; 
    }

    // Makes union of difference of 3 vectors. Returns = vec_0 U (vec_1 - vec_2)
    vector<Instruction *> UnionOfDifference(vector<Instruction *>& vec_0, vector<Instruction *>& vec_1, vector<Instruction *>& vec_2) {
        vector<Instruction *> u;

        for(vector<Instruction*>::iterator elem = vec_1.begin(); elem != vec_1.end(); elem++) {
            u.push_back(*elem);
        }
        for(vector<Instruction*>::iterator elem = vec_2.begin(); elem != vec_2.end(); elem++) {
            if(find(vec_1.begin(), vec_1.end(), *elem) == vec_1.end()) {
                u.push_back(*elem);
            }
        }
        for(vector<Instruction*>::iterator elem = vec_0.begin(); elem != vec_0.end(); elem++) {
            if(find(u.begin(), u.end(), *elem) == u.end()) {
                u.push_back(*elem);
            }
        }
       return u; 
    }

    class VecI {                        // Instruction vectors.
        public:
            vector<Instruction*> use; 
            vector<Instruction*> def; 
            vector<Instruction*> in; 
            vector<Instruction*> out; 
    };

    class VecB : public VecI {          // Block vectors.
        public:
            vector<BasicBlock*> suc;
    };

    class VecL {                        // Live vectors.    
        public:
            map<Instruction*, VecI*> i_vecs;
            map<BasicBlock*, VecB*> b_vecs;

            // Destructor
            ~VecL() {
                // New/Alloc elements in second position of Map.
                for(map<BasicBlock*, VecB*>::iterator i = b_vecs.begin();  i != b_vecs.end(); i++) { 
                    delete i->second;
                }

                b_vecs.clear();

                for(map<Instruction*, VecI*>::iterator i = i_vecs.begin();  i != i_vecs.end(); i++) {
                    delete i->second;
                }

                i_vecs.clear();
            }
    };

    struct livinessP3 : public FunctionPass
    {
        static char ID;
        int removalCount;
        bool changed;

        livinessP3() : FunctionPass(ID) {
            removalCount=0;
            changed = false;
        }

        ~livinessP3() {
            errs() << "Total Instructions Removed: " << removalCount << "\n";
        }

        // Perform liveness analisys and remove Dead Instructions.
        virtual bool runOnFunction(Function &F) {
            bool redoIn = true;
            unsigned operands, opIt, last_size;
            unsigned int l, su;
            Value * aValue;
            Instruction * aInstruction; 
            VecB * aVecB, * succ;
            VecL allVecs;
            vector<Instruction*> removal;
            //bool redoAll = false;

            //do {
                //redoAll = false;
                //allVecs.clear();

                if(debug) {
                    errs() << "Function: " << F.getName().str() << "\n";
                }

                // Part 1: Find .use and .def vectors for Blocks.

                // Allocate memory for DenseMap           
                for(Function::iterator it = F.begin(); it != F.end(); ++it) {                   // Iterate in b_vecs
                    allVecs.b_vecs[&*it] = new VecB();

                    // Add successors to it vector.
                    for(succ_iterator succ = succ_begin(&*it); succ != succ_end(&*it); succ++) {
                        allVecs.b_vecs[&*it]->suc.push_back((BasicBlock *) *succ);
                    }

                    for(BasicBlock::iterator jt = it->begin(); jt != it->end(); ++jt) {         // Iterate in i_vecs
                        if (isa <Instruction>(*jt)) {
                            allVecs.i_vecs[&*jt] = new VecI();
                        }
                    }
                }  

                for(Function::iterator i = F.begin(), e = F.end(); i != e; ++i) {               // Find .USE and .DEF vectors.
                    aVecB = allVecs.b_vecs[&*i];
                    for(BasicBlock::iterator j = i->begin(), e = i->end(); j != e; ++j) {
                        operands = j->getNumOperands();

                        if (j->hasName()) {
                            if (isa<Instruction>(*j)) {
                                if((find(aVecB->use.begin(), aVecB->use.end(), (&*j)) == aVecB->use.end()) 
                                    && (find(aVecB->def.begin(), aVecB->def.end(), (&*j)) == aVecB->def.end())) {
                                    aVecB->def.push_back(&*j);
                                }
                            }
                        }

                        for(opIt = 0; opIt < operands; opIt++) {
                            aValue = j->getOperand (opIt);
                            if (isa <Instruction> (*aValue)) {
                                aInstruction = cast <Instruction> (&*aValue);
                                
                                if((find(aVecB->def.begin(), aVecB->def.end(), (&*aInstruction)) == aVecB->def.end())
                                    && (find(aVecB->use.begin(), aVecB->use.end(), (&*aInstruction)) == aVecB->use.end())) {
                                    aVecB->use.push_back(&*aInstruction);
                                }
                            }
                        }
                    }
                }

                // Part2: Find .IN, .OUT, .DEF and .USE.

                while (redoIn == true) {                    // Loop until IN remain unchanged.
                    redoIn = false;
                    Function::iterator fe = F.end();        // Start at the end.

                    while (fe != F.begin()) {
                        aVecB = allVecs.b_vecs[&*--fe];

                        for(su = 0; su < aVecB->suc.size(); su++) {     // get successors.
                            succ = allVecs.b_vecs[aVecB->suc[su]];

                            for(vector<Instruction*>::iterator elem = succ->in.begin(); elem != succ->in.end(); elem++) {   // Join .in vectors.
                                if(find(aVecB->out.begin(), aVecB->out.end(), *elem) == aVecB->out.end()) {
                                    aVecB->out.push_back(*elem);
                                }
                            }
                        }

                        last_size = aVecB->in.size();                   // Use to check if IN has changed.

                        // IN = USE U (OUT - DEF)
                        aVecB->in = aVecB->use;
                        vector<Instruction *> OutDiffDef = Difference(aVecB->out, aVecB->def);

                        for(vector<Instruction*>::iterator elem = OutDiffDef.begin(); elem != OutDiffDef.end(); elem++) {
                            if(find(aVecB->in.begin(), aVecB->in.end(), *elem) == aVecB->in.end()) {
                                aVecB->in.push_back(*elem);
                            }
                        }

                        if (last_size != aVecB->in.size()) {            // Check if IN has changed.
                            redoIn = true;
                        }
                    }
                }

                for(Function::iterator i = F.begin(); i != F.end(); i++) {
                    for(BasicBlock::iterator j = i->begin(); j != i->end(); j++) {
                        if(isa<Instruction>(*j)) {
                            operands = j->getNumOperands();

                            if ((j->hasName()) && (find(allVecs.i_vecs[&*j]->use.begin(), allVecs.i_vecs[&*j]->use.end(), &*j) == allVecs.i_vecs[&*j]->use.end())) {
                                allVecs.i_vecs[&*j]->def.push_back(&*j);
                            }

                            for(l = 0; l < operands; l++) {
                                aValue = j->getOperand(l);

                                if(isa<Instruction>(aValue)) {
                                    Instruction* op = cast<Instruction>(aValue);

                                    if(find(allVecs.i_vecs[&*j]->def.begin(), allVecs.i_vecs[&*j]->def.end(), op) == allVecs.i_vecs[&*j]->def.end()) {
                                        allVecs.i_vecs[&*j]->use.push_back(op);
                                    }
                                }
                            }
                        }
                    }
                }

                // Part 3 : Verify which elements can be safely removed and remove them.

                for(Function::iterator i = F.begin(); i != F.end(); i++) {
                    BasicBlock::iterator j = i->end();
                    allVecs.i_vecs[&*(--j)]->out = allVecs.b_vecs[&*i]->out;

                    // .IN = .USE U (.OUT - .DEF)
                    allVecs.i_vecs[&*j]->in = UnionOfDifference (allVecs.i_vecs[&*j]->use, allVecs.i_vecs[&*j]->out, allVecs.i_vecs[&*j]->def);
                    BasicBlock::iterator k; 

                    while(j != i->begin()) {    // All but last instruction
                        k = j;

                        allVecs.i_vecs[&*(--j)]->out = allVecs.i_vecs[&*k]->in;

                        // .IN = .USE U (.OUT - .DEF)
                        allVecs.i_vecs[&*j]->in = UnionOfDifference(allVecs.i_vecs[&*j]->use, allVecs.i_vecs[&*j]->out, allVecs.i_vecs[&*j]->def);
                    } 

                    for(BasicBlock::iterator jt = i->begin(); jt != i->end(); jt++) {           // Loop in Instructions  
                        if ((isa<Instruction>(*jt))                                             // Check if it's a instruction
                             &&(!isa<TerminatorInst>(*jt)) && (!isa<LandingPadInst>(*jt))       // Check if may damage. 
                             &&(!jt->mayHaveSideEffects()) && (!isa<DbgInfoIntrinsic>(*jt))     // Check if out = 0.
                             &&(find(allVecs.i_vecs[&*jt]->out.begin(), allVecs.i_vecs[&*jt]->out.end(), &*jt) == allVecs.i_vecs[&*jt]->out.end())) {
                                removal.push_back(&*jt);
                        }
                    }
                }

                for(vector<Instruction*>::iterator elem = removal.begin(); elem != removal.end(); elem++) {
                    if(debug) {
                        errs() << "Removing: " << *(*elem) << "\n";
                    }
                    (*elem)->eraseFromParent();
                }

                removalCount += removal.size();

                if(debug) {
                    errs() << "Instructions removed from function: " << removal.size() << "\n";
                    errs() << "Instructions removed so far: " << removalCount << "\n";
                }

                if(removal.size()>0) { 
                    changed = true;
                    errs() << "Repeating function\n";
                    return runOnFunction(F);
                }

            return changed;
        } 
    }; 
}

char livinessP3::ID = 0;
static RegisterPass<livinessP3> X("dce-liveness", "Liviness Analysis - DCE", false, false);
