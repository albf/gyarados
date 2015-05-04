from subprocess import Popen, PIPE
from subprocess import call

llicommand = '/usr/local/Cellar/llvm/3.4.1/bin/lli'

def check(output, correct, name):
	if(output == correct):
		print name + ': CORRECT'
	else:
		print name + ': PROBLEM'

# Make
call(['make'])

# Run and verify test_arithmetic.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/simple/test_arithmetic.java', 
	'results/test_arithmetic.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/test_arithmetic.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '20\n', 'test_arithmetic.java')

# Run and verify test_class_decl.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/simple/test_class_decl.java', 
	'results/test_class_decl.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/test_class_decl.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '100\n', 'test_class_decl.java')

# Run and verify test_if-simple.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/simple/test_if-simple.java', 
	'results/test_if-simple.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/test_if-simple.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '10\n', 'test_if-simple.java')

# Run and verify test_if-then-else.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/simple/test_if-then-else.java', 
	'results/test_if-then-else.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/test_if-then-else.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '100\n', 'test_if-then-else.java')

# Run and verify m109.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m109.java', 
	'results/m109.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m109.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '10\n', 'm109.java')

# Run and verify m110.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m110.java', 
	'results/m110.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m110.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '10\n', 'm110.java')

# Run and verify m309.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m309.java', 
	'results/m309.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m309.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '11\n', 'm309.java')

# Run and verify m312.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m312.java', 
	'results/m312.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m312.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '9\n', 'm312.java')

# Run and verify m315.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m315.java', 
	'results/m315.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m315.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '0\n', 'm315.java')

# Run and verify m318.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m318.java', 
	'results/m318.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m318.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '1\n', 'm318.java')

# Run and verify m322.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m322.java', 
	'results/m322.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m322.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '1\n', 'm322.java')

# Run and verify m328.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m328.java', 
	'results/m328.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m328.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '10\n', 'm328.java')

# Run and verify m330.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m330.java', 
	'results/m330.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m330.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '0\n', 'm330.java')

# Run and verify m334.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m334.java', 
	'results/m334.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m334.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '1\n', 'm334.java')

# Run and verify m336.java
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/smaller/m336.java', 
	'results/m336.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/m336.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '0\n', 'm336.java')

# Run and verify Fatorial
p = Popen(['java', '-classpath', 'src:lib/projeto2.jar', 'main/Main', 'test/bigger/Factorial.java', 
	'results/Factorial.s'], stdin=PIPE, stdout=PIPE, stderr=PIPE).communicate()
p = Popen([llicommand, 'results/Factorial.s'], stdout = PIPE)
output, err = p.communicate()
check(output, '3628800\n', 'Factorial.java')



