from subprocess import Popen, PIPE
from subprocess import call

llicommand = '/usr/local/Cellar/llvm/3.4.1/bin/lli'

# Make
call(['make'])

# Run and verify Fatorial
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/bigger/Factorial.java', 'results/Factorial.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
output, err = p.communicate()
p = Popen([llicommand, 'results/Factorial.s'], stdout = PIPE)
output, err = p.communicate()
if(output == '3628800\n'):
	print 'FACTORIAL: CORRECT'
else:
	print 'FACTORIAL: PROBLEM'

