#include <algorithm>
#include <vector>

#include "llvm/ADT/DenseMap.h"
#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/IntrinsicInst.h"
#include "llvm/Pass.h"
#include "llvm/Support/CFG.h"
#include "llvm/Support/InstIterator.h"
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
            DenseMap<Instruction*, VecI*> i_vecs;
            DenseMap<BasicBlock*, VecB*> b_vecs;

            // Destructor
            ~VecL() {
                // New/Alloc elements in second position of DenseMap.
                for(DenseMap<BasicBlock*, VecB*>::iterator i = b_vecs.begin();  i != b_vecs.end(); i++) { 
                    delete i->second;
                }

                b_vecs.clear();

                for(DenseMap<Instruction*, VecI*>::iterator i = i_vecs.begin();  i != i_vecs.end(); i++) {
                    delete i->second;
                }

                i_vecs.clear();
            }

            void iAdd(Instruction* new_i) {
                i_vecs[new_i] = new VecI();
            }

            void bAdd(BasicBlock* new_b) {
                b_vecs[new_b] = new VecB();

                for(succ_iterator succ = succ_begin(new_b); succ != succ_end(new_b); succ++) {
                    b_vecs[new_b]->suc.push_back((BasicBlock *) *succ);
                }
            }
    };

    struct livinessP3 : public FunctionPass
    {
        static char ID;
        int removalCount;

        livinessP3() : FunctionPass(ID) {
            removalCount=0;
        }

        ~livinessP3() {
            errs() << "Total Instructions Removed: " << removalCount << "\n";
        }

        // Perform liveness analisys and remove Dead Instructions.
        virtual bool runOnFunction(Function &F) {
            bool changed = false;
            bool redoIn = true;
            unsigned operands, opIt, last_size;
            unsigned int l, su;
            Value * aValue;
            Instruction * aInstruction; 
            VecB * aVecB, * succ;
            VecL allVecs;
            vector<Instruction*> removal;

            if(debug) {
                errs() << "Function: " << F.getName().str() << "\n";
            }

            // Part 1: Find .use and .def vectors for Blocks.
           
            for(Function::iterator it = F.begin(); it != F.end(); ++it) {           // Iterate in b_vecs
                allVecs.bAdd(&*it);

                for(BasicBlock::iterator jt = it->begin(); jt != it->end(); ++jt) {        // Iterate in i_vecs
                    if (isa <Instruction>(*jt)) {
                        allVecs.iAdd(&*jt);
                    }
                }
            }  

            for(Function::iterator i = F.begin(), e = F.end(); i != e; ++i) {
                aVecB = allVecs.b_vecs[&*i];
                for(BasicBlock::iterator j = i->begin(), e = i->end(); j != e; ++j) {
                    operands = j->getNumOperands();

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

                    if (j->hasName()) {
                        if (isa<Instruction>(*j)) {
                            if((find(aVecB->use.begin(), aVecB->use.end(), (&*j)) == aVecB->use.end()) 
                                && (find(aVecB->def.begin(), aVecB->def.end(), (&*j)) == aVecB->def.end())) {
                                aVecB->def.push_back(&*j);
                            }
                        }
                    }
                }
            }

            // Part2: Find .IN, .OUT, .DEF and .USE.

            while (redoIn == true) {                 // Loop until IN remain unchanged.
                redoIn = false;
                Function::iterator fe = F.end();    // Start at the end.

                while (fe != F.begin()) {
                    fe--;
                    aVecB = allVecs.b_vecs[&*fe];

                    for(su = 0; su < aVecB->suc.size(); su++) {    // get successors.
                        succ = allVecs.b_vecs[aVecB->suc[su]];

                        for(vector<Instruction*>::iterator elem = succ->in.begin(); elem != succ->in.end(); elem++) {   // Join .in vectors.
                            if(find(aVecB->out.begin(), aVecB->out.end(), *elem) == aVecB->out.end()) {
                                aVecB->out.push_back(*elem);
                            }
                        }
                    }

                    last_size = aVecB->in.size();           // Use to check if IN has changed.

                    // IN = USE U (OUT - DEF)
                    aVecB->in = aVecB->use;
                    vector<Instruction *> OutDiffDef = Difference(aVecB->out, aVecB->def);

                    for(vector<Instruction*>::iterator elem = OutDiffDef.begin(); elem != OutDiffDef.end(); elem++) {
                        if(find(aVecB->in.begin(), aVecB->in.end(), *elem) == aVecB->in.end()) {
                            aVecB->in.push_back(*elem);
                        }
                    }

                    if (last_size != aVecB->in.size()) {    // Check if IN has changed.
                        redoIn = true;
                    }
                }
            }

            for(Function::iterator i = F.begin(); i != F.end(); i++) {
                for(BasicBlock::iterator j = i->begin(); j != i->end(); j++) {
                    if(isa<Instruction>(*j)) {
                        operands = j->getNumOperands();

                        for(l = 0; l < operands; l++) {
                            aValue = j->getOperand(l);

                            if(isa<Instruction>(aValue)) {
                                Instruction* op = cast<Instruction>(aValue);

                                if(find(allVecs.i_vecs[&*j]->def.begin(), allVecs.i_vecs[&*j]->def.end(), op) == allVecs.i_vecs[&*j]->def.end()) {
                                    allVecs.i_vecs[&*j]->use.push_back(op);
                                }
                            }
                        }

                        if ((j->hasName()) && (find(allVecs.i_vecs[&*j]->use.begin(), allVecs.i_vecs[&*j]->use.end(), &*j) == allVecs.i_vecs[&*j]->use.end())) {
                            allVecs.i_vecs[&*j]->def.push_back(&*j);
                        }
                    }
                }

            }

            for(Function::iterator i = F.begin(); i != F.end(); i++) {
                BasicBlock::iterator j = i->end();
                j--;
                allVecs.i_vecs[&*j]->out = allVecs.b_vecs[&*i]->out;

                // .IN = .USE U (.OUT - .DEF)
                allVecs.i_vecs[&*j]->in = UnionOfDifference (allVecs.i_vecs[&*j]->use, allVecs.i_vecs[&*j]->out, allVecs.i_vecs[&*j]->def);
                BasicBlock::iterator k; 

                while(j != i->begin()) {    // All but last instruction
                    k = j;
                    j--;

                    allVecs.i_vecs[&*j]->out = allVecs.i_vecs[&*k]->in;

                    // .IN = .USE U (.OUT - .DEF)
                    allVecs.i_vecs[&*j]->in = UnionOfDifference(allVecs.i_vecs[&*j]->use, allVecs.i_vecs[&*j]->out, allVecs.i_vecs[&*j]->def);
                } 
            }

            // Part 3 : Verify which elements can be safely removed and remove them.

            for(Function::iterator i = F.begin(); i != F.end(); i++) {                      // Loop in Basic Blocks 
                for(BasicBlock::iterator j = i->begin(); j != i->end(); j++) {              // Loop in Instructions  
                    if ((isa<Instruction>(*j))                                             // Check if it's a instruction
                         &&(!isa<TerminatorInst>(*j)) && (!isa<LandingPadInst>(*j))         // Check if may damage. 
                         &&(!j->mayHaveSideEffects()) && (!isa<DbgInfoIntrinsic>(*j))       // Check if out = 0.
                         &&(find(allVecs.i_vecs[&*j]->out.begin(), allVecs.i_vecs[&*j]->out.end(), &*j) == allVecs.i_vecs[&*j]->out.end())) {
                            removal.push_back(&*j);
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
            }

            return changed;
        } 
    }; 
}

char livinessP3::ID = 0;
static RegisterPass<livinessP3> X("la", "Liviness Analysis - DCE", false, false);
