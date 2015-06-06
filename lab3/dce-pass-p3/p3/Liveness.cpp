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

// =============================
// Liveness Data
// =============================

namespace 
{
    class BasicBlockData 
    {
        public:
            vector<BasicBlock*> sucessors;
            vector<Instruction*> use; 
            vector<Instruction*> def; 
            vector<Instruction*> in; 
            vector<Instruction*> out; 
    };

    class InstructionData 
    {
        public:
            vector<Instruction*> use; 
            vector<Instruction*> def; 
            vector<Instruction*> in; 
            vector<Instruction*> out; 
    };

    // This class contains all data we'll need in liveness analysis 
    class LivenessData 
    {
        public:
            // These vectors contains all data we'll need
            DenseMap< BasicBlock*, BasicBlockData* > blocks;
            DenseMap< Instruction*, InstructionData* > instructions;

            // Destructor
            ~LivenessData() 
            {
                for(DenseMap< BasicBlock*, BasicBlockData* >::iterator i = blocks.begin();  i != blocks.end(); i++) 
                    delete i->second;

                for(DenseMap< Instruction*, InstructionData* >::iterator i = instructions.begin();  i != instructions.end(); i++) 
                    delete i->second;

                blocks.clear();
                instructions.clear();
            }

            // This method stores a new BasicBlock
            void addBasicBlock(BasicBlock* block) 
            {
                blocks[block] = new BasicBlockData();

                // Adding sucessors
                for(succ_iterator succesor = succ_begin(block); succesor != succ_end(block); succesor++) 
                {
                    BasicBlock* Succ = *succesor;
                    blocks[block]->sucessors.push_back(Succ);
                }
            }

            // This method stores a new Instruction
            void addInstruction(Instruction* inst) 
            {
                instructions[inst] = new InstructionData();
            }
    };

    struct dcep3 : public FunctionPass 
    {
        static char ID;

        dcep3() : FunctionPass(ID) {
        }

        ~dcep3()
        {
        }

        string newInst ()
        {
            static int ni = 0;
            std::stringstream ss;
            ss << " tmp" << ni++;
            ////errs() << ss.str() << "\n";
            return ss.str();
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

        // =============================
        // Liveness analysis
        // =============================
        void computeLiveness(Function* func, LivenessData& data) 
        {
            // ===========================================
            // Step 0: Store all BasicBlocks and
            //         Instructions in LivenessData
            // ===========================================
           
            // Iterating on all blocks of the function
            for(Function::iterator i = func->begin(); i != func->end(); ++i) {
                data.addBasicBlock(&*i);

                // Iterating on all instructions of the block
                for (BasicBlock::iterator j = i->begin(), e = i->end(); j != e; ++j) {
                    if (isa < Instruction >(*j)) {
                        data.addInstruction(&*j);
                    }
                }
            }  

            // ===========================================
            // Step 1: Compute use/def for all BasicBLocks
            // ===========================================
            unsigned numOp, opr;
            for (Function::iterator i = func->begin(), e = func->end(); i != e; ++i) {
                BasicBlockData * b = data.blocks[&*i];
                Value * vv;
                for (BasicBlock::iterator j = i->begin(), e = i->end(); j != e; ++j) {
                    numOp = j->getNumOperands();

                    for (opr = 0; opr < numOp; opr++) {
                        vv = j->getOperand (opr);
                        if (isa < Instruction > (*vv)) {
                            Instruction * vvv = cast < Instruction > (&*vv);
                            
                            if((find(b->def.begin(), b->def.end(), (&*vvv)) == b->def.end())
                                && (find(b->use.begin(), b->use.end(), (&*vvv)) == b->use.end())) {
                                b->use.push_back(&*vvv);
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

            // ===========================================
            // Step 2: Compute in/out for all BasicBlocks
            // ===========================================

            // Reversely iterating on blocks
            bool inChanged = true;
            unsigned int last_size;

            while (inChanged == true)
            {
                //LOGC2 ("Loop until every IN isn't changed...\n");
                inChanged = false;
                Function::iterator fe = func->end();

                //for (Function::iterator i = fe, e = func->begin(); i != e;)
                while (fe != func->begin())
                {
                    fe--;
                    BasicBlockData * b = data.blocks[&*fe];

                    // For each successor
                    for (unsigned int s = 0; s < b->sucessors.size(); s++)
                    {
                        BasicBlockData * succ = data.blocks[b->sucessors[s]];

                        // Union in[S]
                        //b->out.insert(b->out.end(), succ->in.begin(), succ->in.end());
                        for(vector<Instruction*>::iterator elem = succ->in.begin(); elem != succ->in.end(); elem++) {
                            if(find(b->out.begin(), b->out.end(), *elem) == b->out.end()) {
                                b->out.push_back(*elem);
                            }
                        }
                        errs() << "b->out.size after. " << b->out.size() << "\n";
                    }

                    // Used to verify if IN will change
                    last_size = b->in.size();

                    b->in = b->use;

                    // tmp = out - def
                    vector<Instruction *> tmp;

                    // Out[B] - defB
                    tmp = Difference(b->out, b->def);

                    // use[B] U (out[B] - def[B])

                    for(vector<Instruction*>::iterator elem = tmp.begin(); elem != tmp.end(); elem++) {
                        if(find(b->in.begin(), b->in.end(), *elem) == b->in.end()) {
                            b->in.push_back(*elem);
                        }
                    }

                    //b->in.insert(b->in.end(), tmp.begin(), tmp.end());

                    // If some IN changed
                    if (last_size != b->in.size())
                    {
                        inChanged = true;
                    }
                }
            }

            // ===========================================
            // Step 3: Use data from BasicBlocks to
            //         compute all Instructions use/def
            // ===========================================

            for(Function::iterator i = func->begin(); i != func->end(); i++) 
            {
                // For every Instruction inside a BasicBlock...
                for (BasicBlock::iterator j = i->begin(); j != i->end(); j++) 
                {
                    if(isa<Instruction>(*j)) 
                    {
                        unsigned int n = j->getNumOperands();

                        for(unsigned int k = 0; k < n; k++)
                        {
                            Value* v = j->getOperand(k);

                            if(isa<Instruction>(v)) 
                            {
                                Instruction* op = cast<Instruction>(v);

                                if(find(data.instructions[&*j]->def.begin(), data.instructions[&*j]->def.end(), op) == data.instructions[&*j]->def.end()) {
                                //if (!data.instructions[&*j]->def.count(op)) 
                                    data.instructions[&*j]->use.push_back(op);
                                }
                            }
                        }

                        if (j->hasName())
                        if(find(data.instructions[&*j]->use.begin(), data.instructions[&*j]->use.end(), &*j) == data.instructions[&*j]->use.end()) {
                        //if (!data.instructions[&*j]->use.count(&*j))
                            data.instructions[&*j]->def.push_back(&*j);
                        }
                    }
                }

            }

            errs() << "Done Step 3\n";
            // ===========================================
            // Step 4: Use data from BasicBLocks to
            //         compute all Instructions in/out
            // ===========================================

            for(Function::iterator i = func->begin(); i != func->end(); i++) 
            {
                // Last instruction of the block
                BasicBlock::iterator j = i->end();
                j--;
                data.instructions[&*j]->out = data.blocks[&*i]->out;

                // in = use U (out - def)
                //data.instructions[&*j]->in = Union(data.instructions[&*j]->use, Difference(data.instructions[&*j]->out, data.instructions[&*j]->def));
                data.instructions[&*j]->in = UnionOfDifference (data.instructions[&*j]->use, data.instructions[&*j]->out, data.instructions[&*j]->def);

                // Other instructions
                BasicBlock::iterator aux;

                // for each instruction other than the last one
                while(j != i->begin())
                {
                    aux = j;
                    j--;

                    data.instructions[&*j]->out = data.instructions[&*aux]->in;

                    // in = use U (out - def)
                    //data.instructions[&*j]->in = Union(data.instructions[&*j]->use, Difference(data.instructions[&*j]->out, data.instructions[&*j]->def));
                    data.instructions[&*j]->in = UnionOfDifference(data.instructions[&*j]->use, data.instructions[&*j]->out, data.instructions[&*j]->def);
                } 
            }
        } 

        // =============================
        // Optimization
        // =============================

        virtual bool runOnFunction(Function &F) 
        {
            errs() << "Optimization done at " << F.getName().str() << "\n";
            bool changed = false;
            LivenessData data;
            vector<Instruction*> toDelete;

            computeLiveness(&F, data);
            errs() << "Live analysis finished.\n";

            ////errs() << "Tamanho do DenseMap: " << data.instructions.size() << "\n";
            
            for(Function::iterator i = F.begin(); i != F.end(); i++) {                      // Loop in Basic Blocks 
                for(BasicBlock::iterator j = i->begin(); j != i->end(); j++) {              // Loop in Instructions  
                    if ((isa<Instruction>(*j))                                             // Check if it's a instruction
                         &&(!isa<TerminatorInst>(*j)) && (!isa<LandingPadInst>(*j))         // Check if may damage. 
                         &&(!j->mayHaveSideEffects()) && (!isa<DbgInfoIntrinsic>(*j))       // Check if out = 0.
                         &&(find(data.instructions[&*j]->out.begin(), data.instructions[&*j]->out.end(), &*j) == data.instructions[&*j]->out.end())) {
                            errs() << "RemovingBF: " << *j << "\n";
                            toDelete.push_back(&*j);
                    }
                }
            }

            errs() << "Instruções deletadas: " << toDelete.size() << "\n";
            for(vector<Instruction*>::iterator elem = toDelete.begin(); elem != toDelete.end(); elem++) {
                errs() << "Removing: " << *(*elem) << "\n";
                (*elem)->eraseFromParent();
            }

            if(toDelete.size()>0) { 
                changed = true;
            }

            errs() << "Exiting " << toDelete.size() << "\n";
            return changed;
        } 
    }; 
}

char dcep3::ID = 0;
static RegisterPass<dcep3> X("dce-p3", "Dead Code Elimination Pass", false, false);
