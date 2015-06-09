from glob import glob as glb
import os
import subprocess as subp

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
def compile():
	for test in glb('../dce-tests/minijava/*.ll'):
		name = test[22:-3]
		output = name+'.opt.ll'

		for opt in flags:
			print 'Optmization {0} | File {1}'.format(opt, name)
			my_cmd = command + opt + ' ' + test + ' > ' + outdir + opt + '_' + output
			os.system(my_cmd)

# ------------------------------------------------------------------------------
def testAll():
	for test in glb('../dce-tests/minijava/*.ll'):
		name = test[22:-3]
		output = name+'.opt.ll'

		liveness = subp.Popen([llicommand, './dce-liveness_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
		ssa 	 = subp.Popen([llicommand, './dce-ssa_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
		outlive, err = liveness.communicate()
		outssa, err = ssa.communicate()
		
		check(outlive, outssa, name)

		diff = subp.Popen(['diff', './dce-ssa_'+output, 'dce-ssa_'+output], stdout=subp.PIPE, stderr=subp.PIPE)
		outdiff, err = diff.communicate()
		check(outdiff, '', 'diff')


# ------------------------------------------------------------------------------
if os.path.isdir("./Release"):
	os.system('make')
else:
	os.system('mkdir Release')
	os.system('make')


if os.path.isdir("./"+outdir):
	print 'Output dir - OK!'
	compile()
	testAll()
else:
	print 'Creating directory: ' + outdir
	os.system('mkdir '+outdir)
	compile()
	testAll()
