#include <algorithm>
#include <vector>
#include <map>
#include <queue>
#include <list>

#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/IntrinsicInst.h"
#include "llvm/Pass.h"
//#include "llvm/Support/CFG.h" // for Moco
#include "llvm/IR/CFG.h" // for IC
//#include "llvm/Support/InstIterator.h"
#include "llvm/IR/InstIterator.h" // for IC
#include "llvm/Support/raw_ostream.h"

using namespace llvm;
using namespace std;

#define debug true

namespace {
    
    void fillWorkList(Function &F, list<Value*> &workList) {

        for(Function::iterator i = F.begin(), e = F.end(); i != e; ++i)
            for (BasicBlock::iterator inst=i->begin(), tinst=i->end(); inst != tinst; ++inst)
                //if (!inst->getName().empty()) {
                    workList.push_back(inst);
                //}
    }

    void addToList(Value * val, list<Value*> &workList) {
        if (!val->getName().empty()) {
            workList.push_back(val);
        }
    }

    bool hasSideEffects(Value *val) {
        bool rValue = false;

        if (Instruction *inst = dyn_cast<Instruction>(val)) {
            if(inst->mayHaveSideEffects()) {
                rValue = true;
            }
        } else if ( isa<TerminatorInst>(*val) ) {
            rValue = true;
        } else if ( isa<DbgInfoIntrinsic>(*val) ) {
            rValue = true;
        } else if (isa<LandingPadInst>(*val)) {
            rValue = true;
        }

        return rValue;
    }

    struct DCE_SSA : public FunctionPass
    {
        static char ID;
        int removalCount;
        vector<int> memRem, iterations;
        
        DCE_SSA() : FunctionPass(ID) {
            removalCount=0;
        }

        ~DCE_SSA() {

            // for (int i=0; i<memRem.size(); ++i) {
            //     errs() << "\t" << memRem[i] << " Instructions removed!\n";
            //     errs() << "\t" << iterations[i] << " Iterations performed!\n";
            //     removalCount+=memRem[i];
            // }

            errs() << "Total Instructions Removed: " << removalCount << "\n";
        }

        // Perform liveness analisys and remove Dead Instructions.
        virtual bool runOnFunction(Function &F) {

            // int dce_count, instRemoved = 0;
            bool changed;

            do {

                /* =================================================================
                 * Part 1: Get a list with all the variables
                 * ============================================================== */
                changed = false;
                list<Value*> workList;
                fillWorkList(F, workList);

                /* =================================================================
                 * Part 2: Check all the variables in the list
                 * ============================================================== */

                /* Make the list processing */
                while(!workList.empty()) {
                    Value *val = workList.front();

                    /* =================================================================
                     * Part 3: Take some variable V at the list and verify if it has
                     * an empty use list
                     * ============================================================== */

                    /* Check if the use list is empty */
                    if (val->use_empty()) {

                        /* =================================================================
                         * Part 4: Check if the statement that defines V has other side
                         * effects
                         * ============================================================== */

                        /* Check for side effects */
                        if (!hasSideEffects(val)) {
                            
                            /* =================================================================
                             * Part 5: Put the variables of statement in the list to be 
                             * check for possible death and delete the statement 
                             * from the program
                             * ============================================================== */

                            /* Put the operands in the list */
                            if (Instruction *inst = dyn_cast<Instruction>(val)) {
                                /* Check for the operands */
                                for (Instruction::op_iterator op = inst->op_begin(), oe = inst->op_end(); op != oe; ++op) {
                                    Value *v = *op;

                                    /* If it's defined by a instruction */
                                    if (isa<Instruction>(*v)) {
                                        addToList(v, workList);
                                    }
                                }

                                // errs() << "Removing: " << *inst << "\n";
                                inst->eraseFromParent();
                                if (!changed) {
                                    changed = true;
                                }
                                // instRemoved++;
                                removalCount++;
                            }
                        }
                    }

                    workList.pop_front();
                }
                // memRem.push_back(instRemoved);
                // instRemoved = 0;
                // dce_count++;

            } while(changed);

            // iterations.push_back(dce_count);

            return false;
        }
    };
}

char DCE_SSA::ID = 0;
static RegisterPass<DCE_SSA> X("dce-ssa", "Single Static Assignment - DCE", false, false);
