#include <algorithm>
#include <fstream>
#include <sstream>
#include <vector>

#include "llvm/ADT/DenseMap.h"
#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/IntrinsicInst.h"
#include "llvm/Pass.h"
#include "llvm/Support/CFG.h"
#include "llvm/Support/InstIterator.h"
#include "llvm/Support/raw_ostream.h"

using namespace llvm;
using namespace std;

#define debug true

namespace {
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
            DenseMap<BasicBlock*, VecB*> b_vecs;
            DenseMap<Instruction*, VecI*> i_vecs;

            // Destructor
            ~VecL() {
                for(DenseMap< BasicBlock*, VecB* >::iterator i = b_vecs.begin();  i != b_vecs.end(); i++) { 
                    delete i->second;
                }

                b_vecs.clear();

                for(DenseMap< Instruction*, VecI* >::iterator i = i_vecs.begin();  i != i_vecs.end(); i++) {
                    delete i->second;
                }

                i_vecs.clear();
            }

            void bAdd(BasicBlock* block) {
                b_vecs[block] = new VecB();

                for(succ_iterator succesor = succ_begin(block); succesor != succ_end(block); succesor++) {
                    BasicBlock* Succ = *succesor;
                    b_vecs[block]->suc.push_back(Succ);
                }
            }

            void iAdd(Instruction* inst) {
                i_vecs[inst] = new VecI();
            }
    };

    struct dcep3 : public FunctionPass
    {
        static char ID;

        dcep3() : FunctionPass(ID) {
        }

        ~dcep3() {
        }

        // Makes Union of vectors.
        vector<Instruction *> Difference(vector<Instruction *>& vec_1, vector<Instruction *>& vec_2) {
            vector<Instruction *> u;

            for(vector<Instruction*>::iterator elem = vec_1.begin(); elem != vec_1.end(); elem++) {
                if(find(vec_2.begin(), vec_2.end(), *elem) == vec_2.end()) {
                    u.push_back(*elem);
                }
            }
           //errs() << "u_size : " << u.size() << "\n";
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

        // Perform liveness analisys and save results in allVecs.
        void livenessAnalysis(Function* func, VecL& allVecs) {
            unsigned operands, opr, last_size;
            bool redoIn = true;
            Value * aValue;
            Instruction * aInstruction; 
            VecB * b, * succ;

            // Part 1: Find .use and .def vectors for Blocks.
           
            for(Function::iterator it = func->begin(); it != func->end(); ++it) {           // Iterate in b_vecs
                allVecs.bAdd(&*it);

                for (BasicBlock::iterator jt = it->begin(); jt != it->end(); ++jt) {        // Iterate in i_vecs
                    if (isa < Instruction >(*jt)) {
                        allVecs.iAdd(&*jt);
                    }
                }
            }  

            for (Function::iterator i = func->begin(), e = func->end(); i != e; ++i) {
                b = allVecs.b_vecs[&*i];
                for (BasicBlock::iterator j = i->begin(), e = i->end(); j != e; ++j) {
                    operands = j->getNumOperands();

                    for (opr = 0; opr < operands; opr++) {
                        aValue = j->getOperand (opr);
                        if (isa <Instruction> (*aValue)) {
                            aInstruction = cast < Instruction > (&*aValue);
                            
                            if((find(b->def.begin(), b->def.end(), (&*aInstruction)) == b->def.end())
                                && (find(b->use.begin(), b->use.end(), (&*aInstruction)) == b->use.end())) {
                                b->use.push_back(&*aInstruction);
                            }
                        }
                    }

                    if (j->hasName()) {
                        if (isa<Instruction>(*j)) {
                            if((find(b->use.begin(), b->use.end(), (&*j)) == b->use.end()) 
                                && (find(b->def.begin(), b->def.end(), (&*j)) == b->def.end())) {
                                b->def.push_back(&*j);
                            }
                        }
                    }
                }
            }

            // Part2: Find .IN, .OUT, .DEF and .USE.

            while (redoIn == true) {                 // Loop until IN remain unchanged.
                redoIn = false;
                Function::iterator fe = func->end();    // Start at the end.

                while (fe != func->begin()) {
                    fe--;
                    b = allVecs.b_vecs[&*fe];

                    for (unsigned int s = 0; s < b->suc.size(); s++) {    // get successors.
                        succ = allVecs.b_vecs[b->suc[s]];

                        for(vector<Instruction*>::iterator elem = succ->in.begin(); elem != succ->in.end(); elem++) {   // Join .in vectors.
                            if(find(b->out.begin(), b->out.end(), *elem) == b->out.end()) {
                                b->out.push_back(*elem);
                            }
                        }
                    }

                    last_size = b->in.size();           // Use to check if IN has changed.

                    // IN = USE U (OUT - DEF)
                    b->in = b->use;
                    vector<Instruction *> tmp = Difference(b->out, b->def);

                    for(vector<Instruction*>::iterator elem = tmp.begin(); elem != tmp.end(); elem++) {
                        if(find(b->in.begin(), b->in.end(), *elem) == b->in.end()) {
                            b->in.push_back(*elem);
                        }
                    }

                    if (last_size != b->in.size()) {    // Check if IN has changed.
                        redoIn = true;
                    }
                }
            }


            for(Function::iterator i = func->begin(); i != func->end(); i++) {
                for (BasicBlock::iterator j = i->begin(); j != i->end(); j++) {
                    if(isa<Instruction>(*j)) {
                        operands = j->getNumOperands();

                        for(unsigned int k = 0; k < operands; k++) {
                            aValue = j->getOperand(k);

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

            for(Function::iterator i = func->begin(); i != func->end(); i++) {
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
        } 

        virtual bool runOnFunction(Function &F) {
            bool changed = false;
            VecL allVecs;
            vector<Instruction*> removal;

            if(debug) {
                errs() << "Function: " << F.getName().str() << "\n";
            }

            livenessAnalysis(&F, allVecs);

            if(debug) {
                errs() << "Live analysis finished.\n";
            }

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

            if(debug) {
                errs() << "Instructions removed: " << removal.size() << "\n";
            }

            if(removal.size()>0) { 
                changed = true;
            }

            return changed;
        } 
    }; 
}

char dcep3::ID = 0;
static RegisterPass<dcep3> X("dce-p3", "Dead Code Elimination Pass", false, false);
