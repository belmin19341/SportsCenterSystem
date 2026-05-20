import {Link} from 'react-router'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'

export function NotFoundPage() {
	return (
		<div className='mx-auto max-w-xl'>
			<Card>
				<CardHeader>
					<CardTitle>Page not found</CardTitle>
					<CardDescription>
						The route does not exist in the first SportsCenter frontend slice.
					</CardDescription>
				</CardHeader>
				<CardContent>
					<Link to='/'>
						<Button>Back to home</Button>
					</Link>
				</CardContent>
			</Card>
		</div>
	)
}
