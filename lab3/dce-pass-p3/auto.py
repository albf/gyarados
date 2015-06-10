from glob import glob as glb
import os
import subprocess as subp
import sys

command = 'opt -S -load Release/P3.so -'
flags = ['dce-liveness', 'dce-ssa']
outdir = 'output/'
llicommand = 'lli'

# ------------------------------------------------------------------------------
def check(output, correct, name):
    if(output == correct):
        print name + ': CORRECT'
    else:
        print name + ': PROBLEM'

# ------------------------------------------------------------------------------
def optimize_minijava():
    for test in glb('../dce-tests/minijava/*.ll'):
        name = test[22:-3]

        for opt in flags:
            output = '../dce-tests/minijava/'+outdir+opt+'_'+name+'.opt.ll'
            print 'Optmization {0} | File {1}'.format(opt, name)
            my_cmd = command + opt + ' ' + test + ' > ' + output
            print my_cmd
            os.system(my_cmd)

# ------------------------------------------------------------------------------
def optimize_gcc(opt):
    for test in glb('../dce-tests/spec/gcc/*.bc'):
        name = test[22:-3]
        my_cmd = command + opt + ' ' + test + ' > ' + output
        os.system(my_cmd)

       # for opt in flags:
       #     output = '../dce-tests/spec/gcc/'+outdir+opt+'_'+name+'.opt.ll'
       #     print 'Optmization {0} | File {1}'.format(opt, name)

# ------------------------------------------------------------------------------
def optimize_deal():
    for test in glb('../dce-tests/spec/dealII/*.bc'):
        name = test[25:-3]

        for opt in flags:
            output = '../dce-tests/spec/dealII/'+outdir+opt+'_'+name+'.opt.ll'
            print 'Optmization {0} | File {1}'.format(opt, name)
            my_cmd = command + opt + ' ' + test + ' > ' + output
            os.system(my_cmd)

# ------------------------------------------------------------------------------
def test_minijava():
    for test in glb('../dce-tests/minijava/*.ll'):
        name = test[22:-3]
        output = name+'.opt.ll'

        liveness = subp.Popen([llicommand, '../dce-test/minijava/output/dce-liveness_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
        ssa      = subp.Popen([llicommand, '../dce-test/minijava/output/dce-ssa_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
        outlive, err = liveness.communicate()
        outssa, err = ssa.communicate()
        
        check(outlive, outssa, name)

        diff = subp.Popen(['diff', './dce-ssa_'+output, 'dce-ssa_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
        outdiff, err = diff.communicate()
        check(outdiff, '', 'diff')
# ------------------------------------------------------------------------------
def test_gcc():
    for test in glb('../dce-tests/spec/gcc/*.bc'):
        name = test[22:-3]
        output = name+'.opt.ll'

        liveness = subp.Popen([llicommand, './dce-liveness_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
        ssa      = subp.Popen([llicommand, './dce-ssa_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
        outlive, err = liveness.communicate()
        outssa, err = ssa.communicate()
        
        check(outlive, outssa, name)

        diff = subp.Popen(['diff', './dce-ssa_'+output, 'dce-ssa_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
        outdiff, err = diff.communicate()
        check(outdiff, '', 'diff')
# ------------------------------------------------------------------------------
def run_minijava():
    if os.path.isdir("../dce-tests/minijava/"+outdir):
        print 'Output dir - OK!'
        optimize_minijava()
    else:
        print 'Creating directory: ' + outdir
        os.system('mkdir ../dce-tests/minijava/'+outdir)
        optimize_minijava()

# ------------------------------------------------------------------------------
def run_gcc():
    if os.path.isdir("../dce-tests/spec/gcc/"+outdir):
        print 'Output dir - OK!'
        optimize_gcc()
    else:
        print 'Creating directory: ' + outdir
        os.system('mkdir ../dce-tests/spec/gcc/'+outdir)
        optimize_gcc()

# ------------------------------------------------------------------------------
def run_deal():
    if os.path.isdir("../dce-tests/spec/dealII/"+outdir):
        print 'Output dir - OK!'
        optimize_deal()
    else:
        print 'Creating directory: ' + outdir
        os.system('mkdir ../dce-tests/spec/deal/'+outdir)
        optimize_deal()
# ------------------------------------------------------------------------------
def main(argv):
    # make the directory and build the .so
    if os.path.isdir("./Release"):
        os.system('make')
    else:
        os.system('mkdir Release')
        os.system('make')

    # run the tests
    for arg in argv:
        if arg == 'minijava':
            run_minijava()
            test_minijava()
        elif arg == 'gcc':
            run_gcc('dce-ssa')
            #test_gcc()
        else:
            run_deal()
            #test_deal()


# set the main as the entry point
if __name__ == '__main__':
    main(sys.argv[1:])
